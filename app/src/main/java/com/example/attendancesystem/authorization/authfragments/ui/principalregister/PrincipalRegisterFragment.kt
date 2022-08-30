package com.example.attendancesystem.authorization.authfragments.ui.principalregister

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.databinding.FragmentPrincipalRegisterBinding
import com.example.attendancesystem.utils.*
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class PrincipalRegisterFragment : Fragment(R.layout.fragment_principal_register) {

    @Inject
    lateinit var glide: RequestManager
    private lateinit var binding: FragmentPrincipalRegisterBinding
    private lateinit var viewModel: PrincipalRegisterViewModel
    private var curImageUri: Uri = Uri.EMPTY
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var curLocation: Location? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[PrincipalRegisterViewModel::class.java]
        subscribeToObserve()

        binding = FragmentPrincipalRegisterBinding.bind(view)

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        } else {
            curLocation = getLocation()
        }

        binding.apply {

            btnRegisterFaculty.setOnClickListener {
                hideKeyboard(requireActivity())
                if (curLocation != null) {
                    viewModel.register(
                        email = etFacultyEmail.text?.trim().toString(),
                        phone = etFacultyPhone.text?.trim().toString(),
                        name = etFacultyName.text?.trim().toString(),
                        enrolNo = etFacultyEnrol.text?.trim().toString(),
                        adminKey = etAdminKey.text?.trim().toString(),
                        pin = pinView.text.toString(),
                        schoolId = etSchoolId.text?.trim().toString(),
                        curLatitude = curLocation!!.latitude,
                        curLongitude = curLocation!!.longitude
                    )
                } else {
                    snackbar("Fetching location, Please wait")
                }
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
                setOutputCompressQuality(30)
                setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                setImageSource(includeGallery = true, includeCamera = true)
            }
        )
    }

    private fun subscribeToObserve() {

        viewModel.curImageUri.observe(viewLifecycleOwner) {
            binding.tvCaptureImage.isVisible = it == Uri.EMPTY
            if (it == Uri.EMPTY) {
                binding.ivImage.setImageResource(R.drawable.ic_round_person_24)
            } else {
                curImageUri = it
                glide.load(curImageUri).into(binding.ivImage)
            }
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
                        binding.etFacultyEmail.error = "Email cannot be empty"
                    }
                    "email" -> {
                        binding.etFacultyEmail.error = "Enter a valid email"
                    }
                    "emptyPhone" -> {
                        binding.etFacultyPhone.error = "Phone number cannot be empty"
                    }
                    "phone" -> {
                        binding.etFacultyPhone.error = "Enter a valid phone number"
                    }
                    "name" -> {
                        binding.etFacultyName.error = "Name cannot be empty"
                    }
                    "emptyEnrol" -> {
                        binding.etFacultyEnrol.error = "Enrolment number cannot be empty"
                    }
                    "enrol" -> {
                        binding.etFacultyEnrol.error = "Enter a valid enrolment number"
                    }
                    "pin" -> {
                        snackbar("Pin should be of 6 length")
                    }
                    "uri" -> {
                        snackbar("Capture your image")
                    }
                    "emptySchoolID" -> {
                        binding.etSchoolId.error = "School Id cannot be empty"
                    }
                    else -> snackbar(it)
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
                etFacultyEmail.setText("")
                etFacultyName.setText("")
                etFacultyEnrol.setText("")
                etFacultyPhone.setText("")
                etSchoolId.setText("")
                pinView.setText("")
            }
            val sharedPreferences: SharedPreferences =
                requireActivity().getSharedPreferences(
                    Constants.SHARED_PREF_FILE,
                    Context.MODE_PRIVATE
                )

            val editor: SharedPreferences.Editor = sharedPreferences.edit()

            editor.apply {
                putString(Constants.LATITUDE_KEY, user.schoolLatitude.toString())
                putString(Constants.LONGITUDE_KEY, user.schoolLongitude.toString())
                apply()
                commit()
            }

            apiRequestFaceDataUpload(
                user = user
            )
        })
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                curLocation = getLocation()
            } else {
                val alertDialog = AlertDialog.Builder(requireContext()).apply {
                    setTitle("Location Permission")
                    setMessage("The app couldn't function without the location permission.")
                    setCancelable(false)
                    setPositiveButton("ALLOW") { dialog, _ ->
                        dialog.dismiss()
                        requestLocationPermission()
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

    private fun getLocation(): Location? {

        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return null
        } else {
            locationListener = LocationListener {

            }
            return try {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    locationListener
                )
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: Exception) {
                Log.d("TAG_ERROR", "getLocation: ${e.message}")
                null
            }
        }
    }

    private fun apiRequestFaceDataUpload(user: User) {

        val client = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        try {

            val requestBody: RequestBody = FormBody.Builder()
                .add("role", "principal")
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
                    Log.d(
                        "TAG_API_CALL", "onResponse: ${
                            JSONObject(
                                response.peekBody(2048).string()
                            ).get("message")
                        }"
                    )
                    if (response.isSuccessful) {
                        if (JSONObject(
                                response.peekBody(2048).string()
                            ).get("message") == "Person Already Exists!"
                        ) {
                            requireActivity().runOnUiThread {
                                showError("Person already exists with this face")
                            }
                            return
                        }
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
            PrincipalRegisterFragmentDirections
                .actionPrincipalRegisterFragmentToFaceLoginFragment(role = "principal")
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

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentImageUri(Uri.EMPTY)
        locationManager.removeUpdates(locationListener)
    }

}