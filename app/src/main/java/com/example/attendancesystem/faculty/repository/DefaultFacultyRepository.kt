package com.example.attendancesystem.faculty.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.utils.Constants
import com.example.attendancesystem.utils.Resource
import com.example.attendancesystem.utils.safeCall
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*


class DefaultFacultyRepository : FacultyRepository {

    private val auth = FirebaseAuth.getInstance()

    //    private lateinit var mAuth2: FirebaseAuth
//    private val storage = Firebase.storage
    private val students = FirebaseFirestore.getInstance().collection("student")
    private val faculties = FirebaseFirestore.getInstance().collection("faculty")
//    private lateinit var imageUploadResult: UploadTask.TaskSnapshot

    override suspend fun addStudent(
        context: Context,
        profilePicUrl: Uri,
        bitmap: Bitmap,
        email: String,
        name: String,
        enrolNo: String
    ): Resource<User> {
        return withContext(Dispatchers.IO) {
            safeCall {

//                val firebaseOptions = FirebaseOptions.Builder()
//                    .setDatabaseUrl("https://attendance-system-e6d33-default-rtdb.asia-southeast1.firebasedatabase.app/")
//                    .setApiKey("AIzaSyC_hCmXK2hfOMy9cLiwltciTRo0V0lfnGs")
//                    .setApplicationId("attendance-system-e6d33").build()
//
//                mAuth2 = try {
//                    val myApp = FirebaseApp.initializeApp(
//                        context,
//                        firebaseOptions,
//                        "Attendance Auth 2"
//                    )
//                    FirebaseAuth.getInstance(myApp)
//                } catch (e: IllegalStateException) {
//                    FirebaseAuth.getInstance(FirebaseApp.getInstance("Attendance Auth 2"))
//                }
//
//                val result =
//                    mAuth2.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD_STUDENT).await()
//                try {
                val uid = UUID.randomUUID().toString()
//                    val imgID = UUID.randomUUID().toString()
//                    imageUploadResult =
//                        storage.getReference("students/$UNIVERSITY_NAME/$enrolNo/$imgID")
//                            .putFile(profilePicUrl)
//                            .await()
//
//                    val imageUrl =
//                        imageUploadResult.metadata?.reference?.downloadUrl?.await().toString()

                val curUser = getUser().data!!

                val baos = ByteArrayOutputStream()

                val resizedBitmap = Bitmap.createScaledBitmap(bitmap,
                    Constants.DST_WIDTH,
                    Constants.DST_HEIGHT, true)

                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val byteArray: String =
                    Base64.encodeToString(
                        baos.toByteArray(),
                        Base64.DEFAULT
                    )

                val user = User(
                    enrollment = enrolNo,
                    name = name,
                    email = email,
                    uid = uid,
                    school_id = curUser.school_id,
                    byteArray = byteArray
                )

                students.document(uid).set(user).await()

//                    mAuth2.currentUser?.updateProfile(
//                        userProfileChangeRequest {
//                            displayName = name
//                            photoUri = Uri.parse(imageUrl)
//                        }
//                    )
//                    mAuth2.signOut()


//                    apiRequestFaceDataUpload(imageUrl, enrolNo, name)
//
//                } catch (e: Exception) {
//                    Log.d("TAG_REGISTER_STUDENT", "registerStudent: $e")
//                }
                Resource.Success(user)
            }
        }
    }

    override suspend fun getUser(): Resource<User> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val user = faculties.document(auth.currentUser?.uid!!).get().await()
                    .toObject(User::class.java)!!
                Resource.Success(user)
            }
        }
    }

    override suspend fun getUsersList(): Resource<List<User>> {
        return withContext(Dispatchers.IO) {
            safeCall {
                var userList = listOf<User>()
                try {
                    userList = students
                        .orderBy("enrollment", Query.Direction.ASCENDING)
                        .get()
                        .await()
                        .toObjects(User::class.java)
                } catch (e: Exception) {
                    Log.d("TAG_STUDENT_LIST", "getUsersList: ${e.message}")
                }
                Resource.Success(userList)
            }
        }
    }
}