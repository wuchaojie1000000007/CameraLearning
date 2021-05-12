package com.example.camera

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.fragment.app.FragmentContainerView

class MainActivity : AppCompatActivity() {
    private lateinit var container: FragmentContainerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    companion object {
        private const val TAG = "MainActivity"
        fun getOutputFileDirectory(context: Context) = context.applicationContext.filesDir
    }
}