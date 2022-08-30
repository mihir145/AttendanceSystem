package com.example.attendancesystem.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.attendancesystem.R
import com.google.android.material.snackbar.Snackbar

fun hideKeyboard(activity: Activity) {
    val inputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    val currentFocusedView = activity.currentFocus
    currentFocusedView?.let {
        inputMethodManager.hideSoftInputFromWindow(
            currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}

fun showKeyboard(activity: Activity, view: View) {
    val inputMethodManager: InputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    view.requestFocus()
    inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun showProgress(
    activity: Activity,
    bool: Boolean,
    parentLayout: ConstraintLayout,
    cvProgress: CardView
) {
    cvProgress.isVisible = bool
    if (bool) {
        parentLayout.alpha = 0.5f
        activity.window!!.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    } else {
        parentLayout.alpha = 1f
        activity.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

}

fun Fragment.snackbar(text: String) {
    Snackbar.make(
        requireView(),
        text,
        Snackbar.LENGTH_LONG
    ).show()
}