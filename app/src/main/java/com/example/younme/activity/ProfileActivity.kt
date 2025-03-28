package com.example.younme.activity

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.example.younme.R
import com.example.younme.adapter.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : AppCompatActivity() {

    private lateinit var userNameTextView: TextView
    private lateinit var profileImageView: CircleImageView
    private lateinit var btnAvatar: Button
    private lateinit var btnName: Button
    private lateinit var mDbRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var storageReference: StorageReference

    private var uri: Uri? = null

    private val captureImage = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            uri?.let { uploadImage(it) }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            this.uri = it
            uploadImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val btnSetting= findViewById<Button>(R.id.btn_setting)
        btnSetting.setOnClickListener{
            val i = Intent(this@ProfileActivity,SettingsActivity::class.java)
            startActivity(i)
        }

        userNameTextView = findViewById(R.id.user_name)
        profileImageView = findViewById(R.id.profile_image)
        btnAvatar = findViewById(R.id.btn_avatar)
        btnName = findViewById(R.id.btn_name)


        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference("user")
        storageReference = FirebaseStorage.getInstance().reference

        val currentUser = mAuth.currentUser
        currentUser?.let {
            val uid = it.uid
            mDbRef.child(uid).get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    userNameTextView.text = user.name
                    loadProfileImage(uid)
                }
            }
        }

        btnAvatar.setOnClickListener {
            showImagePickerDialog()
        }

        btnName.setOnClickListener {
            showChangeNameDialog()
        }
    }

    private fun showChangeNameDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 40, 50, 10)

        val nameEditText = EditText(this)
        nameEditText.hint = getString(R.string.enter_name)
        layout.addView(nameEditText)

        AlertDialog.Builder(this)
            .setTitle(R.string.change_name)
            .setView(layout)
            .setPositiveButton("OK") { dialog, _ ->
                val newName = nameEditText.text.toString()
                updateUserName(newName)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateUserName(newName: String) {
        val currentUser = mAuth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            mDbRef.child(userId).child("name").setValue(newName)
                .addOnSuccessListener {
                    userNameTextView.text = newName
                    Toast.makeText(this, R.string.update_name, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, R.string.update_name_fail, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(getString(R.string.takephoto), getString(R.string.choosephoto), getString(R.string.cancel))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.choose_profile_pic)
        builder.setItems(options) { dialog, item ->
            when (options[item]) {
                getString(R.string.takephoto) -> {
                    val photoUri = createImageUri()
                    captureImage.launch(photoUri)
                    uri = photoUri
                }
                getString(R.string.choosephoto) -> {
                    pickImage.launch("image/*")
                }
                getString(R.string.cancel) -> {
                    dialog.dismiss()
                }
            }
        }
        builder.show()
    }


    private fun createImageUri(): Uri? {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "new_image.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun uploadImage(uri: Uri) {
        val currentUser = mAuth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val imageRef = storageReference.child("avatars/$userId.jpg")
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                        mDbRef.child(userId).child("profileImageUrl").setValue(imageUrl.toString())
                        loadProfileImage(userId)
                        Toast.makeText(this, R.string.upload_image_suc, Toast.LENGTH_SHORT).show()
                    }
                        .addOnFailureListener {
                            Toast.makeText(this, R.string.upload_image_fail, Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, R.string.upload_image_fail, Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun loadProfileImage(userId: String) {
        mDbRef.child(userId).child("profileImageUrl").get().addOnSuccessListener { snapshot ->
            val profileImageUrl = snapshot.getValue(String::class.java)
            if (profileImageUrl != null) {
                Glide.with(this).load(profileImageUrl).into(profileImageView)
            } else {
                Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show()
        }
    }
}

