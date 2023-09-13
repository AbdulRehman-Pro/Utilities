package com.rehman.utilsdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.rehman.utilities.Utils
import com.rehman.utilities.Utils.showToast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showToast("Hellow")
    }
}