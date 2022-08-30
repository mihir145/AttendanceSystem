package com.example.attendancesystem.faculty.ui.takeattendance

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
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
import com.example.attendancesystem.data.entity.Attendance
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.databinding.FragmentTakeAttendanceStudentBinding
import com.example.attendancesystem.utils.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class TakeAttendanceFragment : Fragment(R.layout.fragment_take_attendance_student) {

    @Inject
    lateinit var glide: RequestManager
    private lateinit var binding: FragmentTakeAttendanceStudentBinding
    private lateinit var viewModel: TakeAttendanceViewModel
    private val attendance = FirebaseFirestore.getInstance().collection("attendance")
    private var curImageUri: Uri = Uri.EMPTY

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TakeAttendanceViewModel::class.java]
        subscribeToObserve()

        binding = FragmentTakeAttendanceStudentBinding.bind(view)

        binding.apply {

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

            btnUploadImage.setOnClickListener {
                viewModel.getUser()
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
                setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                setImageSource(includeGallery = false, includeCamera = true)
            }
        )
    }

    private fun subscribeToObserve() {

        viewModel.curImageUri.observe(viewLifecycleOwner) {
            binding.tvCaptureImage.isVisible = it == Uri.EMPTY
            binding.btnUploadImage.isVisible = it != Uri.EMPTY
            if (it == Uri.EMPTY) {
                binding.ivImage.setImageResource(R.drawable.ic_round_person_24)
            } else {
                curImageUri = it
                glide.load(curImageUri).into(binding.ivImage)
            }
        }

        viewModel.userStatus.observe(viewLifecycleOwner, EventObserver(
            onError = { error ->
                showProgress(
                    activity = requireActivity(),
                    bool = false,
                    parentLayout = binding.parentLayout,
                    cvProgress = binding.cvProgress
                )
                snackbar(error)
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
            apiRequestTakeAttendance(user)
        })

    }

    private fun apiRequestTakeAttendance(user: User) {

        val client = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val bitmap =
                    BitmapUtils.getBitmapFromUri(requireActivity().contentResolver, curImageUri)

                val baos = ByteArrayOutputStream()

                val resizedBitmap = Bitmap.createScaledBitmap(bitmap,
                    Constants.DST_WIDTH,
                    Constants.DST_HEIGHT, true)

                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val imageEncoded: String =
                    Base64.encodeToString(
                        baos.toByteArray(),
                        Base64.DEFAULT
                    )

                val uid = UUID.randomUUID().toString()

                val att = Attendance(
                    uid = uid,
                    byteArray = imageEncoded
                )

                attendance.document(uid).set(att).await()


                val requestBody: RequestBody = FormBody.Builder()
                    .add("school_id", user.school_id)
                    .add("role", "student")
                    .add("uid", uid)
                    .build()

                val request: Request = Request.Builder()
                    .url("${Constants.API_URL}/take-attendance")
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
    }

    private fun onSuccess(msg: String) {
        showProgress(
            activity = requireActivity(),
            bool = false,
            parentLayout = binding.parentLayout,
            cvProgress = binding.cvProgress
        )
        snackbar(msg)
        findNavController().navigateUp()
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

    override fun onDetach() {
        super.onDetach()
        viewModel.setCurrentImageUri(Uri.EMPTY)
    }

}