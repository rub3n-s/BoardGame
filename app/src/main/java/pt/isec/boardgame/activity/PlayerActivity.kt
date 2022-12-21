package pt.isec.boardgame.activity

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import pt.isec.boardgame.MainActivity
import pt.isec.boardgame.R
import pt.isec.boardgame.databinding.ActivityPlayerBinding
import pt.isec.boardgame.model.PlayerModel
import pt.isec.boardgame.utils.createFileFromUri
import pt.isec.boardgame.utils.setPic
import java.util.*

class PlayerActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ConfigImageActivity"
        private const val ACTIVITY_REQUEST_CODE_GALLERY = 10
        private const val ACTIVITY_REQUEST_CODE_CAMERA  = 20
        private const val PERMISSIONS_REQUEST_CODE = 1

        private const val GALLERY = 1
        private const val CAMERA  = 2
        private const val MODE_KEY = "mode"
    }

    private lateinit var binding : ActivityPlayerBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private lateinit var storage : FirebaseStorage

    private var mode = GALLERY
    private var imagePath : String? = null
    private var permissionsGranted = false
        set(value) {
            field = value
            binding.userImage.isEnabled = value
        }
    private lateinit var dialog : AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = AlertDialog.Builder(this)
            .setMessage("Updating Profile...")
            .setCancelable(false)

        auth =  Firebase.auth
        database = Firebase.database
        storage = Firebase.storage

        binding.userImage.setOnClickListener { chooseImage() }

        binding.btnConfirm.setOnClickListener {
            if (binding.etPlayerName.text.isEmpty())
                Toast.makeText(applicationContext,"Please enter your Name",Toast.LENGTH_LONG).show()
            else if (imagePath == null)
                Toast.makeText(applicationContext,"Please select your Image",Toast.LENGTH_LONG).show()
            else {
                val player = PlayerModel(imagePath.toString(),binding.etPlayerName.text.toString(),0,0,0)
                startActivity(GameActivity.getClientModeIntent(this, player))
                finish()
            }
        }

        verifyPermissions()
        updatePreview()
    }

    private var startActivityForContentResult = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        Log.i(TAG, "startActivityForContentResult: ")
        imagePath = uri?.let { createFileFromUri(this, it) }
        updatePreview()
    }

    private fun chooseImage() {
        Log.i(TAG, "chooseImage: ")
        startActivityForContentResult.launch("image/*")
    }

    private fun updatePreview() {
        if (imagePath != null) {
            setPic(binding.userImage, imagePath!!)
        }
    }

    // ===================================================
    //        Permissions to access CAMERA/MEDIA
    // ===================================================
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsGranted = isGranted
    }

    // this function supports recent APIs
    private fun verifyPermissions() {
        Log.i(TAG, "verifyPermissions: ")
        if (mode == CAMERA) {
            permissionsGranted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionsGranted)
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            return
        }
        //mode == GALLERY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsGranted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionsGranted)
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            return
        }
        // GALLERY, versões < API33
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED /*||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED */
        ) {
            permissionsGranted = false
            requestPermissionsLauncher.launch(
                arrayOf(
                    //android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        } else
            permissionsGranted = true
    }

    val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
        permissionsGranted = grantResults.values.any { it }
    }

    /*private fun uploadData() {
        val reference = storage.reference.child("Profile").child(Date().time.toString())
        reference.putFile(selectedImg).addOnCompleteListener {
            if (it.isSuccessful) {
                reference.downloadUrl.addOnSuccessListener { task ->
                    uploadInfo(task.toString())
                }
            }
        }
    }

    private fun uploadInfo(imgUrl: String) {
        val player = PlayerModel(auth.uid.toString(),imgUrl,binding.etPlayerName.toString(),0,0,0)

        database.reference.child("players")
            .child(auth.uid.toString())
            .setValue(player)
            .addOnSuccessListener {
                Toast.makeText(this,"Data inserted", Toast.LENGTH_SHORT).show()

                startActivity(GameActivity.getClientModeIntent(this, player))

                finish()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            if (data.data != null) {
                selectedImg = data.data!!

                binding.userImage.setImageURI(selectedImg)
            }
        }
    } */
}