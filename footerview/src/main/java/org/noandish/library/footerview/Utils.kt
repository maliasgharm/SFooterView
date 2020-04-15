package org.noandish.library.footerview

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver


object Utils {
    val widthScreen: Int
        get() = Resources.getSystem().displayMetrics.widthPixels
    val heightScreen: Int
        get() = Resources.getSystem().displayMetrics.heightPixels


    fun ViewGroup.requestSize(listener: (Array<Int>) -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    viewTreeObserver.removeGlobalOnLayoutListener(this)
                } else {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                val width: Int = measuredWidth
                val height: Int = measuredHeight
                listener.invoke(arrayOf(width, height))
            }
        })
    }

    fun View.requestSize(listener: (Array<Int>) -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            @SuppressLint("ObsoleteSdkInt")
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    viewTreeObserver.removeGlobalOnLayoutListener(this)
                } else {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                val width: Int = measuredWidth
                val height: Int = measuredHeight
                listener.invoke(arrayOf(width, height))
            }
        })
    }
}