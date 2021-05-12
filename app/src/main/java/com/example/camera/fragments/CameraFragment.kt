package com.example.camera.fragments

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.example.camera.MainActivity
import com.example.camera.R
import com.example.camera.databinding.FragmentCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var preview: Preview
    private lateinit var outputFileDirectory: File
    private lateinit var camera: Camera

    private val viewFinder by lazy { binding.viewFinder }
    private val captureButton by lazy { binding.captureButton }
    private val switchButton by lazy { binding.switchButton }
    private val galleryButton by lazy { binding.galleryButton }

    private val executor = Executors.newSingleThreadExecutor()

    private var lensFacing = CameraSelector.LENS_FACING_BACK


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize binging
        binding = FragmentCameraBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onResume() {
        super.onResume()
        // User may change permissions before resume
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(CameraFragmentDirections.actionCameraFragmentToPermissionsFragment())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        outputFileDirectory = MainActivity.getOutputFileDirectory(requireContext())

        viewFinder.post {

            updateCameraUI()

            bindCameraUseCases()
        }

    }

    private fun updateCameraUI() {

        switchButton.setOnClickListener {
            // Change lensFacing and rebind camera use cases
            lensFacing =
                if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK
            bindCameraUseCases()
        }
        captureButton.setOnClickListener {
            // Get a stable reference of the modifiable image capture use case
            imageCapture.let { imageCapture ->

                // Create the output file to hold image
                val photoFile = createFile(outputFileDirectory, FILE_NAME, PHOTO_EXTENSION)

                // Set up image capture metadata
                val metadata = ImageCapture.Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }
                // Create output options object which contain files + metadata
                val outputFileOptions = ImageCapture.OutputFileOptions
                    .Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

                imageCapture.takePicture(
                    outputFileOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                            Log.d(TAG, "Photo capture succeed: $savedUri (file path: $photoFile)")
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.d(TAG, "Image capture failed: ${exception.message}", exception)
                        }
                    })
            }

        }
    }

    private fun bindCameraUseCases() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()

            // Use camera provider to check which camera does the device have
            lensFacing = when {
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.LENS_FACING_BACK
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("No camera find!")
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            preview = Preview.Builder()
                .build()
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

            imageCapture = ImageCapture.Builder().build()

            cameraProvider.unbindAll()
            try {
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Bind camera use case failed.", Toast.LENGTH_LONG)
                    .show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    companion object {
        private const val TAG = "CameraFragment"
        private const val FILE_NAME = "'IMG'_yyyyMMdd_HHmmss'.jpg'"
        private const val PHOTO_EXTENSION = ".jpg"
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder,
                SimpleDateFormat(FILE_NAME, Locale.CHINA)
                    .format(System.currentTimeMillis())
            )
    }
}