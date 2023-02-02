package com.example.dynamicdelivery

import android.content.Context
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitcompat.SplitCompatApplication

class DynamicDeliveryApplication: SplitCompatApplication() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (base != null) {
            SplitCompat.install(base)
        }
    }
}