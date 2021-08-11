package com.colisa.podplay.extensions

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.colisa.podplay.R
import com.colisa.podplay.util.Event
import com.colisa.podplay.util.EventObserver
import com.google.android.material.snackbar.Snackbar

fun View.showSnackbar(text: String, time: Int) {
    val snack = Snackbar.make(this, text, time)
    snack.setBackgroundTint(ContextCompat.getColor(context, R.color.teal_700))
    snack.show()
}

fun View.showToast(text: String, time: Int) {
    Toast.makeText(context, text, time).show()
}


fun View.setupSnackbar(
    lifecycleOwner: LifecycleOwner,
    snackbarEvent: LiveData<Event<String>>,
    time: Int
) {
    snackbarEvent.observe(lifecycleOwner, EventObserver {
        showSnackbar(it, time)
    })
}

fun View.hideKeyboard() {
    val input = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    input.hideSoftInputFromWindow(windowToken, 0)
}