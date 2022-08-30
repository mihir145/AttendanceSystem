package com.example.attendancesystem.data.entity

data class User(
    val enrollment: String = "",
    val name: String = "",
    val face_encoding: ArrayList<String> = arrayListOf(),
    val mobile: String = "",
    val email: String = "",
    val uid: String = "",
    val school_id: String = "",
    val byteArray: String = "",
    val schoolLatitude: Double = 0.0,
    val schoolLongitude: Double = 0.0
)
