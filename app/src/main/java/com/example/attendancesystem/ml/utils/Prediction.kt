package com.example.attendancesystem.ml.utils

import android.graphics.Rect

data class Prediction(var bbox: Rect, var label: String, var maskLabel: String = "")
