package com.example.camera.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.camera.MainActivity
import com.example.camera.databinding.FragmentCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private lateinit var preview: Preview
    private lateinit var outputFileDirectory: File
    private lateinit var camera: Camera

    private val viewFinder by lazy { binding.viewFinder }
    private val captureButton by lazy { binding.captureButton }
    private val switchButton by lazy { binding.switchButton }
    private val thumbnail by lazy { binding.thumbnail }

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

    /**
     * Check if user has all permission when fragment resume
     */
    override fun onResume() {
        super.onResume()
        // User may change permissions before resume
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            //Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            findNavController().navigate(CameraFragmentDirections.actionCameraFragmentToPermissionsFragment())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        outputFileDirectory = MainActivity.getOutputFileDirectory(requireContext())


        viewFinder.post {

            updateCameraUI()

            setUpCamera()
        }

    }

    /**
     * Update UI and set clickListener for each button
     */
    private fun updateCameraUI() {

        updateSwitchButton()

        // Update gallery thumbnail
        outputFileDirectory.listFiles { file ->
            file.extension.toUpperCase(Locale.ROOT) in WHITELIST_EXTENSION
        }?.maxOrNull()?.let {
            setGalleryThumbnail(Uri.fromFile(it))
        }

        // Change lensFacing and rebind camera useCases
        switchButton.setOnClickListener {
            // Change lensFacing and rebind camera use cases
            lensFacing =
                if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK

            bindCameraUseCases()
        }

        // Capture image and store it in the output file directory
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

                            // After capture an image, update thumbnail with file's uri
                            setGalleryThumbnail(savedUri)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.d(TAG, "Image capture failed: ${exception.message}", exception)
                        }
                    })
            }
        }

        // todo: I need a new fragment to show my pictures
        thumbnail.setOnClickListener {

        }
    }

    /**
     * Create cameraProvider, then bind camera useCases
     */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()

            lensFacing = when {
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.LENS_FACING_BACK
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("No camera was found.")
            }

            bindCameraUseCases()

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Create cameraSelector and useCases(preview, imageCapture, imageAnalysis)
     * unbind previous useCases, rebind with new one.
     */
    private fun bindCameraUseCases() {

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

    }

    /**
     * Set gallery thumbnail with file uri
     */
    private fun setGalleryThumbnail(uri: Uri) {
        Glide.with(thumbnail).load(uri).centerCrop().into(thumbnail)
    }

    /**
     * If user has both back and front camera, then display switch button,
     * else set it invisible
     */
    private fun updateSwitchButton() {
        switchButton.visibility = if (hasBackCamera() && hasFrontCamera()) View.VISIBLE
        else View.GONE
    }

    /**
     * Check if has back camera
     */
    private fun hasBackCamera() = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)

    /**
     * Check if has front camera
     */
    private fun hasFrontCamera() = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)


    companion object {
        private const val TAG = "CameraFragment"

        // String in the single quote do not map to real number
        private const val FILE_NAME = "'IMG'_yyyyMMdd_HHmmss'.jpg'"
        private const val PHOTO_EXTENSION = ".jpg"

        private val WHITELIST_EXTENSION = arrayOf("JPG")

        private fun createFile(baseFolder: File, format: String, extension: String) = File(
            baseFolder,
            SimpleDateFormat(FILE_NAME, Locale.CHINA).format(System.currentTimeMillis())
        )
    }
}