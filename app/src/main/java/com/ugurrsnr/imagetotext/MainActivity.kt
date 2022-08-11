package com.ugurrsnr.imagetotext

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ugurrsnr.imagetotext.databinding.ActivityMainBinding
import java.lang.Exception




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

    private lateinit var progressDialog : ProgressDialog

    private lateinit var textRecognizer : TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //initialize views
        inputImageBtn = binding.inputImageBtn
        recognizeTextButton = binding.recognizeTextBtn
        imageIV = binding.imageIV
        recognizedTextET = binding.recognizedTextET
        //

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)



        inputImageBtn.setOnClickListener {
            showInputImageDialog()
        }

        recognizeTextButton.setOnClickListener {
            if (imageUri == null) {
                showToast("Pick Image")
            }else{
                recognizeTextFromImage()
            }
        }

        //initialize arrays of permissions
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    }
    //if all the permissions are granted, we can pick the image and recognize the text on it.
    private fun recognizeTextFromImage() {
        progressDialog.setMessage("Preparing")
        progressDialog.show()

        try {
            val inputImage = InputImage.fromFilePath(this,imageUri!!)
            progressDialog.setMessage("Recognizing text...")

            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    val recognizedText = it.text
                    recognizedTextET.setText(recognizedText)
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    showToast("Failed recognize text due to ${e.message}")
                }
        }catch (e:Exception){
            progressDialog.dismiss()
            showToast("Failed to prepare image due to ${e.message}")
        }
    }

    //popUp menu and item click funcs
    private fun showInputImageDialog() {
        val popUpMenu = PopupMenu(this,inputImageBtn) //take image'a basınca iki seçenek çıkar
        popUpMenu.menu.add(Menu.NONE,1,1,"CAMERA")
        popUpMenu.menu.add(Menu.NONE,2,2,"GALLERY")

        popUpMenu.show()

        popUpMenu.setOnMenuItemClickListener { menuItem ->
            val id = menuItem.itemId
            if (id ==1){

                if (checkCameraPermission()){
                    pickImageCamera()
                }else{
                    requestCameraPermission()
                }

            }else if(id == 2){

                if (checkStoragePermission()){
                    pickImageGallery()
                }else{
                    requestStoragePermission()
                }
            }

            return@setOnMenuItemClickListener true
        }

    }

    //two funcs check the permissions
    private fun checkStoragePermission() : Boolean { //izin verildiyse true döner
        return ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission() : Boolean{ //izin verildiyse true döner
        val cameraResult = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return cameraResult && storageResult
    }
    //

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

    //launcher to camera
    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if ( result.resultCode == Activity.RESULT_OK){
                //image came from camera
                imageIV.setImageURI(imageUri)
            }else{
                showToast("Canceled...")

            }
        }
    //launcher to gallery
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




    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermissions, STORAGE_REQUEST_CODE)
    }
    private fun requestCameraPermission()  {
        ActivityCompat.requestPermissions(this,cameraPermissions, CAMERA_REQUEST_CODE)

    }

    //when we request to take permission, according to request code pickImageCamera or pickImageGallery will work.
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

//https://www.youtube.com/watch?v=VigFgq7h2X0&ab_channel=AtifPervaiz