package com.example.attendancesystem.authorization.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.attendancesystem.data.entity.User
import com.example.attendancesystem.utils.Resource
import com.google.firebase.auth.AuthResult

interface AuthRepository {

    suspend fun registerFaculty(
        profilePicUrl: Uri,
        bitmap: Bitmap,
        email: String,
        phone: String,
        name: String,
        enrolNo: String,
        pin: String,
        schoolId: String,
        schoolLatitude: Double,
        schoolLongitude: Double
    ): Resource<User>

    suspend fun registerPrincipal(
        profilePicUrl: Uri,
        bitmap: Bitmap,
        email: String,
        phone: String,
        name: String,
        enrolNo: String,
        pin: String,
        schoolId: String,
        curLatitude: Double,
        curLongitude: Double
    ): Resource<User>

    suspend fun loginUser(
        email: String,
        pin: String
    ): Resource<Pair<String, User>>

    suspend fun login(
        email: String,
        pin: String
    ): Resource<String>

}