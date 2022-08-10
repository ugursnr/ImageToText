package com.ugurrsnr.imagetotext

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.ugurrsnr.imagetotext.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding  : ActivityMainBinding

    private lateinit var inputImageBtn : MaterialButton
    private lateinit var recognizeTextButton : MaterialButton
    private lateinit var imageIV : ImageView
    private lateinit var recognizedTextET : EditText

    private companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101

    }

    private var imageUri : Uri? = null

    private lateinit var cameraPermissions : Array<String>
    private lateinit var storagePermissions : Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //initialize views
        inputImageBtn = binding.inputImageBtn
        recognizeTextButton = binding.recognizeTextBtn
        imageIV = binding.imageIV
        recognizedTextET = binding.recognizedTextET

        inputImageBtn.setOnClickListener {
            showInputImageDialog()
        }

        //initialize arrays of permissions
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    }

    private fun showInputImageDialog() {

    }

    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"

        galleryActivityResultLauncher.launch(intent)
    }

    private fun pickImageCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        cameraActivityResultLauncher.launch(intent)

    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if ( result.resultCode == Activity.RESULT_OK){

                //image came from camera
                imageIV.setImageURI(imageUri)
            }else{
                showToast("Canceled...")

            }
        }


    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if ( result.resultCode == Activity.RESULT_OK){
                val data = result.data
                imageUri = data!!.data

                imageIV.setImageURI(imageUri)
            }else{
                showToast("Canceled...")

            }

        }


    private fun checkStoragePermission() : Boolean {
        return ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission() : Boolean{
        val cameraResult = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return cameraResult && storageResult
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermissions, STORAGE_REQUEST_CODE)
    }
    private fun requestCameraPermission()  {
        ActivityCompat.requestPermissions(this,cameraPermissions, CAMERA_REQUEST_CODE)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && storageAccepted){
                        pickImageCamera()
                    }else{
                        showToast("Permissions required..")
                    }
                }
            }

            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted){
                        pickImageGallery()
                    }else{
                        showToast("Permissions required..")
                    }
                }
            }
        }
    }

    private fun showToast(message : String){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show()
    }
}