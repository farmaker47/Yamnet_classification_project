package com.soloupis.yamnet_classification_project.view

/*Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.soloupis.yamnet_classification_project.R
import com.soloupis.yamnet_classification_project.databinding.FragmentSecondBinding
import com.soloupis.yamnet_classification_project.viewmodel.ListeningFragmentViewmodel
import org.koin.android.viewmodel.ext.android.viewModel

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ListeningFragment : Fragment() {

    private lateinit var binding:FragmentSecondBinding
    private val viewmodel:ListeningFragmentViewmodel by viewModel()

    // Permissions
    var PERMISSION_ALL = 123
    // App saves .wav audio file inside external storage of phone so anyone can compare
    // results with the colab notebook output. For that purpose this permission is mandatory
    var PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSecondBinding.inflate(inflater)
        binding.lifecycleOwner = this

        initialize()




        return binding.root
    }

    private fun hasPermissions(
        context: Context?,
        vararg permissions: String?
    ): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission!!
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun initialize() {
        if (!hasPermissions(activity, *PERMISSIONS)) {
            requestPermissions(PERMISSIONS, PERMISSION_ALL)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {

        if (requestCode == PERMISSION_ALL) {
            if (allPermissionsGranted(grantResults)) {

                Toast.makeText(
                    activity,
                    getString(R.string.allPermissionsGranted),
                    Toast.LENGTH_LONG
                ).show()


            } else {

                Toast.makeText(
                    activity,
                    getString(R.string.permissionsNotGranted),
                    Toast.LENGTH_LONG
                ).show()

                activity?.finish()

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun allPermissionsGranted(grantResults: IntArray) = grantResults.all {
        it == PackageManager.PERMISSION_GRANTED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}