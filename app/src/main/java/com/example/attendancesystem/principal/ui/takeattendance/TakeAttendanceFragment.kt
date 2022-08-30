package com.example.attendancesystem.principal.ui.takeattendance

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.attendancesystem.R
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.databinding.FragmentTakeAttendanceFacultyBinding
import com.example.attendancesystem.ml.model.FaceNetModel
import com.example.attendancesystem.ml.model.Models
import com.example.attendancesystem.ml.utils.FileReader
import com.example.attendancesystem.ml.utils.FrameAnalyserAttendance
import com.example.attendancesystem.utils.Constants
import com.example.attendancesystem.utils.showProgress
import com.example.attendancesystem.utils.snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class TakeAttendanceFragment : Fragment(R.layout.fragment_take_attendance_faculty) {

    private lateinit var binding: FragmentTakeAttendanceFacultyBinding
    private lateinit var viewModel: TakeAttendanceViewModel
    private val args: TakeAttendanceFragmentArgs by navArgs()

    private val faculties = FirebaseFirestore.getInstance().collection("faculty")

    private lateinit var frameAnalyser: FrameAnalyserAttendance
    private lateinit var faceNetModel: FaceNetModel

    private lateinit var fileReader: FileReader
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    // Use the device's GPU to perform faster computations.
    private val useGpu = true

    // Use XNNPack to accelerate inference.
    private val useXNNPack = true

    private val modelInfo = Models.FACENET


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[TakeAttendanceViewModel::class.java]

        binding = FragmentTakeAttendanceFacultyBinding.bind(view)

        binding.apply {

            bboxOverlay.setWillNotDraw(false)
            bboxOverlay.setZOrderOnTop(true)

            faceNetModel = FaceNetModel(requireContext(), modelInfo, useGpu, useXNNPack)
            frameAnalyser = FrameAnalyserAttendance(requireContext(), bboxOverlay, faceNetModel)
            fileReader = FileReader(faceNetModel)

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestCameraPermission()
            } else {
                startCameraPreview()
            }

            start()

        }

    }

    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)
        val imageFrameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageFrameAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser)
        cameraProvider.bindToLifecycle(
            viewLifecycleOwner,
            cameraSelector,
            preview,
            imageFrameAnalysis
        )
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startCameraPreview()
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

    private fun start() {

        CoroutineScope(Dispatchers.IO).launch {

            val faculties = faculties.whereEqualTo("school_id", args.schoolId).get().await()
                .toObjects(User::class.java)

            val images = ArrayList<Pair<String, Bitmap>>()

            for (faculty in faculties) {
                val decodedByteArray: ByteArray = Base64.decode(faculty.byteArray, Base64.DEFAULT)
                val bitmap =
                    BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
                images.add(Pair(faculty.enrollment, bitmap))
            }

            fileReader.run(images, fileReaderCallback)

        }
    }

    private val fileReaderCallback = object : FileReader.ProcessCallback {
        override fun onProcessCompleted(
            data: ArrayList<Pair<String, FloatArray>>,
            numImagesWithNoFaces: Int
        ) {
            frameAnalyser.run(data, frameAnalyserCallback)
        }
    }

    private val frameAnalyserCallback = object : FrameAnalyserAttendance.ResultCallback {
        override fun onResultGot(name: String) {
            requireActivity().runOnUiThread {
                cameraProviderFuture.get().unbindAll()
                showProgress(
                    activity = requireActivity(),
                    bool = true,
                    parentLayout = binding.parentLayout,
                    cvProgress = binding.cvProgress
                )
            }
            apiRequestFacultyAttendance(name)
        }
    }

    private fun apiRequestFacultyAttendance(name: String) {

        val client = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val requestBody: RequestBody = FormBody.Builder()
                    .add("role", "faculty")
                    .add("school_id", args.schoolId)
                    .add("type", args.type)
                    .add("known_face", name)
                    .build()

                val request: Request = Request.Builder()
                    .url("${Constants.API_URL}/take-faculty-attendance")
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
                                onSuccess("Attendance registered for $name")
                            }
                        } else {
                            Log.d("TAG_API_CALL", "onResponse: ${response.body()?.string()}")
                            requireActivity().runOnUiThread {
                                showError("Something went wrong")
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
        startCameraPreview()
    }

    private fun showError(msg: String) {
        showProgress(
            activity = requireActivity(),
            bool = false,
            parentLayout = binding.parentLayout,
            cvProgress = binding.cvProgress
        )
        snackbar(msg)
        startCameraPreview()
    }

}