package com.example.attendancesystem.ml.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.attendancesystem.ml.model.FaceNetModel
import com.example.attendancesystem.ml.model.MaskDetectionModel
import com.example.attendancesystem.utils.BitmapUtils
import com.example.attendancesystem.utils.Constants.NO_MASK
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

class FrameAnalyserAttendance(
    context: Context,
    private var boundingBoxOverlay: BoundingBoxOverlay,
    private var model: FaceNetModel
) : ImageAnalysis.Analyzer {

    private lateinit var callback: ResultCallback

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

    private val nameScoreHashmap = HashMap<String, ArrayList<Float>>()
    private var subject = FloatArray(model.embeddingDim)

    // Used to determine whether the incoming frame should be dropped or processed.
    private var isProcessing = false

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<String, FloatArray>>()

    private val maskDetectionModel = MaskDetectionModel(context)


    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "l2"

    // Use this variable to enable/disable mask detection.
    private val isMaskDetectionOn = true


    init {
        boundingBoxOverlay.drawMaskLabel = isMaskDetectionOn
    }

    fun run(data: ArrayList<Pair<String, FloatArray>>, callback: ResultCallback) {
        faceList = data
        this.callback = callback
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        // If the previous frame is still being processed, then skip this frame
        if (isProcessing || faceList.size == 0) {
            image.close()
            return
        } else {
            isProcessing = true

            // Rotated bitmap for the FaceNet model
            val frameBitmap =
                BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)

            // Configure frameHeight and frameWidth for output2overlay transformation matrix.
            if (!boundingBoxOverlay.areDimsInit) {
                boundingBoxOverlay.frameHeight = frameBitmap.height
                boundingBoxOverlay.frameWidth = frameBitmap.width
            }

            val inputImage =
                InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    CoroutineScope(Dispatchers.Default).launch {
                        runModel(faces, frameBitmap)
                    }
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }

    interface ResultCallback {
        fun onResultGot(name: String)
    }

    private suspend fun runModel(faces: List<Face>, cameraFrameBitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            val predictions = ArrayList<Prediction>()
            for (face in faces) {
                try {
                    // Crop the frame using face.boundingBox.
                    // Convert the cropped Bitmap to a ByteBuffer.
                    // Finally, feed the ByteBuffer to the FaceNet model.
                    val croppedBitmap =
                        BitmapUtils.cropRectFromBitmap(cameraFrameBitmap, face.boundingBox)
                    subject = model.getFaceEmbedding(croppedBitmap)

                    // Perform face mask detection on the cropped frame Bitmap.
                    var maskLabel = ""
                    if (isMaskDetectionOn) {
                        maskLabel = maskDetectionModel.detectMask(croppedBitmap)
                    }

                    // Continue with the recognition if the user is not wearing a face mask
                    if (maskLabel == NO_MASK) {
                        // Perform clustering ( grouping )
                        // Store the clusters in a HashMap. Here, the key would represent the 'name'
                        // of that cluster and ArrayList<Float> would represent the collection of all
                        // L2 norms/ cosine distances.
                        for (i in 0 until faceList.size) {
                            // If this cluster ( i.e an ArrayList with a specific key ) does not exist,
                            // initialize a new one.
                            if (nameScoreHashmap[faceList[i].first] == null) {
                                // Compute the L2 norm and then append it to the ArrayList.
                                val p = ArrayList<Float>()
                                if (metricToBeUsed == "cosine") {
                                    p.add(cosineSimilarity(subject, faceList[i].second))
                                } else {
                                    p.add(l2Norm(subject, faceList[i].second))
                                }
                                nameScoreHashmap[faceList[i].first] = p
                            }
                            // If this cluster exists, append the L2 norm/cosine score to it.
                            else {
                                if (metricToBeUsed == "cosine") {
                                    nameScoreHashmap[faceList[i].first]?.add(
                                        cosineSimilarity(
                                            subject,
                                            faceList[i].second
                                        )
                                    )
                                } else {
                                    nameScoreHashmap[faceList[i].first]?.add(
                                        l2Norm(
                                            subject,
                                            faceList[i].second
                                        )
                                    )
                                }
                            }
                        }

                        // Compute the average of all scores norms for each cluster.
                        val avgScores = nameScoreHashmap.values.map { scores ->
                            scores.toFloatArray().average()
                        }

                        val names = nameScoreHashmap.keys.toTypedArray()
                        nameScoreHashmap.clear()

                        // Calculate the minimum L2 distance from the stored average L2 norms.
                        val bestScoreUserName: String = if (metricToBeUsed == "cosine") {
                            // In case of cosine similarity, choose the highest value.
                            if (avgScores.maxOrNull()!! > model.model.cosineThreshold) {
                                names[avgScores.indexOf(avgScores.maxOrNull()!!)]
                            } else {
                                "Unknown"
                            }
                        } else {
                            // In case of L2 norm, choose the lowest value.
                            if (avgScores.minOrNull()!! > model.model.l2Threshold) {
                                "Unknown"
                            } else {
                                names[avgScores.indexOf(avgScores.minOrNull()!!)]
                            }
                        }
                        predictions.add(
                            Prediction(
                                face.boundingBox,
                                bestScoreUserName,
                                maskLabel
                            )
                        )
                        if (bestScoreUserName != "Unknown") {
                            callback.onResultGot(bestScoreUserName)
                        }
                    } else {
                        // Inform the user to remove the mask
                        predictions.add(
                            Prediction(
                                face.boundingBox,
                                "Please remove the mask",
                                maskLabel
                            )
                        )
                    }
                } catch (e: Exception) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Log.e("Model", "Exception in FrameAnalyser : ${e.message}")
                    continue
                }
            }
            withContext(Dispatchers.Main) {
                // Clear the BoundingBoxOverlay and set the new results ( boxes ) to be displayed.
                boundingBoxOverlay.faceBoundingBoxes = predictions
                boundingBoxOverlay.invalidate()
                isProcessing = false
            }
        }
    }


    // Compute the L2 norm of ( x2 - x1 )
    private fun l2Norm(x1: FloatArray, x2: FloatArray): Float {
        return sqrt(x1.mapIndexed { i, xi -> (xi - x2[i]).pow(2) }.sum())
    }


    // Compute the cosine of the angle between x1 and x2.
    private fun cosineSimilarity(x1: FloatArray, x2: FloatArray): Float {
        val mag1 = sqrt(x1.mapIndexed { _, it -> it * it }.sum())
        val mag2 = sqrt(x2.mapIndexed { _, it -> it * it }.sum())
        val dot = x1.mapIndexed { i, xi -> xi * x2[i] }.sum()
        return dot / (mag1 * mag2)
    }

}