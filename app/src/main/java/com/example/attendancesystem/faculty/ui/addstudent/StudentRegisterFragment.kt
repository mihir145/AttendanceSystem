package com.example.attendancesystem.faculty.ui.addstudent

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.attendancesystem.R
import com.example.attendancesystem.authorization.authfragments.ui.principalregister.PrincipalRegisterFragmentDirections
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.databinding.FragmentStudentRegisterBinding
import com.example.attendancesystem.utils.*
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class StudentRegisterFragment : Fragment(R.layout.fragment_student_register) {

    @Inject
    lateinit var glide: RequestManager
    private lateinit var binding: FragmentStudentRegisterBinding
    private lateinit var viewModel: StudentRegisterViewModel
    private var curImageUri: Uri = Uri.EMPTY

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[StudentRegisterViewModel::class.java]
        subscribeToObserve()

        binding = FragmentStudentRegisterBinding.bind(view)

        binding.apply {

            btnRegisterStudent.setOnClickListener {
                hideKeyboard(requireActivity())
                viewModel.register(
                    email = etStudentEmail.text?.trim().toString(),
                    name = etStudentName.text?.trim().toString(),
                    enrolNo = etStudentEnrol.text?.trim().toString(),
                )
            }

            ivImage.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestCameraPermission()
                } else {
                    startCrop()
                }
            }
        }
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startCrop()
            } else {
                val alertDialog = AlertDialog.Builder(requireContext()).apply {
                    setTitle("Camera Permission")
                    setMessage("The app couldn't function without the camera permission.")
                    setCancelable(false)
                    setPositiveButton("ALLOW") { dialog, _ ->
                        requestCameraPermission()
                        dialog.dismiss()
                    }
                    setNegativeButton("CLOSE") { dialog, _ ->
                        dialog.dismiss()
                        requireActivity().finish()
                    }
                    create()
                }
                alertDialog.show()
            }

        }


    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            curImageUri = result.uriContent!!
            viewModel.setCurrentImageUri(curImageUri)
        } else {
            val exception = result.error
            snackbar(exception.toString())
        }
    }

    private fun startCrop() {
        cropImage.launch(
            options {
                setGuidelines(CropImageView.Guidelines.ON)
                setAspectRatio(1, 1)
                setCropShape(CropImageView.CropShape.OVAL)
                setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                setImageSource(includeGallery = true, includeCamera = true)
            }
        )
    }

    private fun subscribeToObserve() {

        viewModel.removeObservers()

        viewModel.curImageUri.observe(viewLifecycleOwner) {
            if (it == Uri.EMPTY) {
                binding.ivImage.setImageResource(R.drawable.ic_round_person_24)
            } else {
                curImageUri = it
                glide.load(curImageUri).into(binding.ivImage)
            }
            binding.tvCaptureImage.isVisible = it == Uri.EMPTY
        }

        viewModel.registerStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                showProgress(
                    activity = requireActivity(),
                    bool = false,
                    parentLayout = binding.parentLayout,
                    cvProgress = binding.cvProgress
                )
                when (it) {
                    "emptyEmail" -> {
                        binding.etStudentEmail.error = "Email cannot be empty"
                    }
                    "email" -> {
                        binding.etStudentEmail.error = "Enter a valid email"
                    }
                    "name" -> {
                        binding.etStudentName.error = "Name cannot be empty"
                    }
                    "emptyEnrol" -> {
                        binding.etStudentEnrol.error = "Enrolment number cannot be empty"
                    }
                    "enrol" -> {
                        binding.etStudentEnrol.error = "Enter a valid enrolment number"
                    }
                    "pin" -> {
                        snackbar("Pin should be of 6 length")
                    }
                    "uri" -> {
                        snackbar("Capture your image")
                    }
                    else -> {
                        Log.d("TAG_STUDENT_REGISTER", "subscribeToObserve: $it")
                        snackbar(it)
                    }
                }
            },
            onLoading = {
                showProgress(
                    activity = requireActivity(),
                    bool = true,
                    parentLayout = binding.parentLayout,
                    cvProgress = binding.cvProgress
                )
            }
        ) { user ->
            binding.apply {
                etStudentEmail.setText("")
                etStudentName.setText("")
                etStudentEnrol.setText("")
                viewModel.setCurrentImageUri(Uri.EMPTY)
            }
            apiRequestFaceDataUpload(user = user)
        })
    }

    private fun apiRequestFaceDataUpload(user: User) {

        val client = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        try {

            val requestBody: RequestBody = FormBody.Builder()
                .add("role", "student")
                .add("uid", user.uid)
                .build()

            val request: Request = Request.Builder()
                .url("${Constants.API_URL}/add-faculty")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requireActivity().runOnUiThread {
                        if (e.message?.trim().toString() == "timeout")
                            onSuccess("The system is taking longer time to mark attendance, it will get updated in background!")
                        else
                            showError(e.message.toString())
                    }
                    Log.d("TAG_API_CALL", "onFailure: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        requireActivity().runOnUiThread {
                            onSuccess("Successfully registered!!")
                        }
                    } else {
                        Log.d("TAG_API_CALL", "onResponse: ${response.message()}")
                        requireActivity().runOnUiThread {
                            showError(response.message().toString())
                        }
                    }
                }
            })
        } catch (e: java.lang.Exception) {
            Log.d("TAG_API_CALL", "apiRequestTakeAttendance: ${e.message}")
            requireActivity().runOnUiThread {
                showError(e.message.toString())
            }
        }

    }

    private fun onSuccess(msg: String) {
        showProgress(
            activity = requireActivity(),
            bool = false,
            parentLayout = binding.parentLayout,
            cvProgress = binding.cvProgress
        )
        snackbar(msg)
        findNavController().navigate(
            StudentRegisterFragmentDirections
                .actionStudentRegisterFragmentToFacultyHomeFragment()
        )
    }

    private fun showError(msg: String) {
        showProgress(
            activity = requireActivity(),
            bool = false,
            parentLayout = binding.parentLayout,
            cvProgress = binding.cvProgress
        )
        snackbar(msg)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setCurrentImageUri(Uri.EMPTY)
    }

}