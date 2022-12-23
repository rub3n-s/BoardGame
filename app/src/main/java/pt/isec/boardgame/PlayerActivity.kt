package pt.isec.boardgame

import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import pt.isec.boardgame.activity.MenuActivity
import pt.isec.boardgame.databinding.ActivityPlayerBinding
import pt.isec.boardgame.model.PlayerModel
import pt.isec.boardgame.utils.createFileFromUri
import pt.isec.boardgame.utils.setPic


class PlayerActivity : AppCompatActivity() {
    private lateinit var binding : ActivityPlayerBinding
    private lateinit var auth : FirebaseAuth

    private var imagePath : String? = null
    private lateinit var imageUri : Uri
    private var permissionsGranted = false
        set(value) {
            field = value
            binding.userImage.isEnabled = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.userImage.setOnClickListener { chooseImage() }

        binding.btnConfirm.setOnClickListener {
            if (binding.etPlayerName.text.isEmpty())
                Toast.makeText(applicationContext,"Please enter your Name",Toast.LENGTH_LONG).show()
            else if (imagePath == null)
                Toast.makeText(applicationContext,"Please select your Image",Toast.LENGTH_LONG).show()
            else {
                val user: FirebaseUser? = auth.currentUser
                if (user != null) {
                    uploadToFirestore(
                        this,
                        binding.etPlayerName.text.toString(),
                        imageUri
                    )
                } else {
                    signInAnonymously()
                }

                val player = PlayerModel(imagePath.toString(),binding.etPlayerName.text.toString(),0,0,0)
                startActivity(MenuActivity.getPlayerIntent(this,player))
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
        if (uri != null) {
            imageUri = uri
            imagePath = createFileFromUri(this, uri)
            updatePreview()
        }
    }

    private fun chooseImage() {
        Log.i(TAG, "chooseImage: ")
        startActivityForContentResult.launch("image/*")
    }

    private fun updatePreview() {
        if (imagePath != null)
            setPic(binding.userImage, imagePath!!)
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

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
        permissionsGranted = grantResults.values.any { it }
    }

    private fun uploadToFirestore(context: Context, playerName: String, imageURI: Uri) : String? {
        // creating a storage reference
        val storageRef = Firebase.storage.reference

        val imageFileName = "users/profilePic${System.currentTimeMillis()}.png"
        val uploadTask = storageRef.child(imageFileName).putFile(imageURI)
        var imageDownloadURL : String? = null

        uploadTask
            .addOnSuccessListener {
                val downloadURLTask = storageRef.child(imageFileName).downloadUrl
                downloadURLTask
                    .addOnSuccessListener {
                        imageDownloadURL = it.toString()
                        Log.i(TAG, "Image Path: $it")
                    }
                    .addOnFailureListener {
                        Log.i(TAG, "Failed to get image path")
                    }
                Log.i(TAG, "uploadToFirestore: Image uploaded successfully")
            }
            .addOnFailureListener {
                Log.i(TAG, "uploadToFirestore: Image was not uploaded")
            }
        return imageDownloadURL
    }

    private fun signInAnonymously() {
        auth.signInAnonymously().addOnSuccessListener(this) {
            uploadToFirestore(
                this,
                binding.etPlayerName.text.toString(),
                imageUri
            )
        }
            .addOnFailureListener(this
            ) { exception ->
                Log.e(
                    TAG,
                    "signInAnonymously:FAILURE",
                    exception
                )
            }
    }
}