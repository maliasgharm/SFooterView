package org.noandish.library.footerview.utils

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator

class BackgroundAnimationFooter(view1 : View, view2 : View) {

    val objectAnimator = ObjectAnimator.ofFloat(view1, View.ROTATION, 0.0f, 360.0f)
    val objectAnimator2 = ObjectAnimator.ofFloat(view2, View.ALPHA, 0.1f, 1f, 0.1f)

    fun start(){
        objectAnimator.duration = 2000
        objectAnimator.repeatCount = Animation.INFINITE
        objectAnimator.interpolator = LinearInterpolator()
        objectAnimator.start()
        objectAnimator2.duration = 2000
        objectAnimator2.repeatCount = Animation.INFINITE
        objectAnimator2.start()
    }

    fun cancel() {
        objectAnimator.cancel()
        objectAnimator2.cancel()
    }
}