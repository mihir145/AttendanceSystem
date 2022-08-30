package com.example.attendancesystem.authorization.authfragments.ui.faceloginscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
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
import androidx.navigation.fragment.navArgs
import com.example.attendancesystem.R
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.databinding.FragmentFaceLoginBinding
import com.example.attendancesystem.faculty.FacultyActivity
import com.example.attendancesystem.ml.model.FaceNetModel
import com.example.attendancesystem.ml.model.Models
import com.example.attendancesystem.ml.utils.FileReader
import com.example.attendancesystem.ml.utils.FrameAnalyser
import com.example.attendancesystem.principal.PrincipalActivity
import com.example.attendancesystem.utils.Constants
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.Executors

@AndroidEntryPoint
class FaceLoginFragment : Fragment(R.layout.fragment_face_login) {

    private lateinit var binding: FragmentFaceLoginBinding

    //    private val storage = Firebase.storage
    private val principals = FirebaseFirestore.getInstance().collection("principal")
    private val faculties = FirebaseFirestore.getInstance().collection("faculty")

    private val args: FaceLoginFragmentArgs by navArgs()

    private lateinit var frameAnalyser: FrameAnalyser
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

        binding = FragmentFaceLoginBinding.bind(view)

        binding.apply {

            bboxOverlay.setWillNotDraw(false)
            bboxOverlay.setZOrderOnTop(true)

            faceNetModel = FaceNetModel(requireContext(), modelInfo, useGpu, useXNNPack)
            frameAnalyser = FrameAnalyser(requireContext(), bboxOverlay, faceNetModel)
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
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
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
            val curUser: User = if (args.role == "principal") {
                principals.document(Firebase.auth.currentUser!!.uid).get().await()
                    .toObject(User::class.java)!!
            } else {
                faculties.document(Firebase.auth.currentUser!!.uid).get().await()
                    .toObject(User::class.java)!!
            }
            val enrolNo = curUser.enrollment

            val decodedByteArray: ByteArray = Base64.decode(curUser.byteArray, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)

            val images = ArrayList<Pair<String, Bitmap>>()
            images.add(Pair(enrolNo, bitmap))

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

    private val frameAnalyserCallback = object : FrameAnalyser.ResultCallback {
        override fun onResultGot() {

            val sharedPreferences: SharedPreferences =
                requireActivity().getSharedPreferences(
                    Constants.SHARED_PREF_FILE,
                    Context.MODE_PRIVATE
                )

            val editor: SharedPreferences.Editor = sharedPreferences.edit()

            val calendar: Calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.AM_PM, Calendar.AM)
            val tomorrow: Date = calendar.time

            editor.apply {
                putLong(Constants.TIME_KEY, tomorrow.time)
                apply()
                commit()
            }

            if (args.role == "principal") {
                Intent(requireActivity(), PrincipalActivity::class.java).also { intent ->
                    startActivity(intent)
                    requireActivity().finish()
                }
            } else {
                Intent(requireActivity(), FacultyActivity::class.java).also { intent ->
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }

    }

}