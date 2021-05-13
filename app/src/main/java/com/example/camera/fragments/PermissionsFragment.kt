package com.example.camera.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.camera.R
import kotlinx.coroutines.launch

/**
 * The purpose of this fragment is to request permissions, once use granted
 * permissions, then navigate to camera fragment.
 */
class PermissionsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermissions(requireContext())) {
            // Request for permissions
            requestPermissions(PERMISSIONS_REQUEST, PERMISSIONS_REQUEST_CODE)
        } else {
            // Already has permissions, navigate to camera fragment
            navigateToCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission request granted.", Toast.LENGTH_LONG)
                    .show()
                navigateToCamera()
            } else {
                Toast.makeText(requireContext(), "Permission request denied.", Toast.LENGTH_LONG)
                    .show()
            }
        }

    }

    // Navigate to camera fragment
    private fun navigateToCamera() {

        // I think don't need to launch a coroutine to navigate to camera fragment, just call it as normal
        //findNavController().navigate(PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment())

        // todo: learning different between lifecycleScope.launchWhen...
        lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment())
        }
    }

    companion object {

        private val PERMISSIONS_REQUEST = arrayOf(Manifest.permission.CAMERA)
        private const val PERMISSIONS_REQUEST_CODE = 1

        // Check if all permissions granted
        fun hasPermissions(context: Context) = PERMISSIONS_REQUEST.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}