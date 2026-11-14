package com.example.braceletphotos

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var nfc: NfcReader
    private var tagId by mutableStateOf<String?>(null)
    private var folders by mutableStateOf<List<File>>(emptyList())
    private var photos by mutableStateOf<List<File>>(emptyList())
    private var repo by mutableStateOf<DriveRepo?>(null)

    private val signIn = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        runCatching { GoogleSignIn.getSignedInAccountFromIntent(r.data).result }
            .getOrNull()?.email?.let { onSignedIn(it) }
    }
    private val notifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        nfc = NfcReader(this) { id ->
            tagId = id
            lifecycleScope.launch { Prefs.set(this@MainActivity, Prefs.TAG_ID, id) }
        }
        setContent { MaterialTheme { Screen() } }
    }

    override fun onResume() { super.onResume(); nfc.start() }
    override fun onPause() { super.onPause(); nfc.stop() }

    private fun startSignIn() {
        val opts = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestScopes(Scope(DriveScopes.DRIVE_READONLY)).build()
        signIn.launch(GoogleSignIn.getClient(this, opts).signInIntent)
    }

    private fun onSignedIn(email: String) {
        getSharedPreferences("auth", 0).edit().putString("account", email).apply()
        val r = DriveRepo(this, email)
        repo = r
        lifecycleScope.launch {
            try { folders = r.listFolders() }
            catch (e: UserRecoverableAuthIOException) { startActivity(e.intent) }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun linkFolder(f: File) = lifecycleScope.launch {
        Prefs.set(this@MainActivity, Prefs.FOLDER_ID, f.id)
        Prefs.set(this@MainActivity, Prefs.LAST_CHECK, java.time.Instant.now().toString())
        photos = repo!!.photosIn(f.id)
        PhotoCheckWorker.schedule(this@MainActivity)
    }

    @Composable
    private fun Screen() = Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Bracelet: ${tagId ?: "Tap your bracelet to the phone"}",
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (repo == null) Button(onClick = { startSignIn() }) { Text("Sign in with Google") }
        else if (photos.isEmpty()) {
            Text("Choose the folder linked to this bracelet:")
            LazyColumn { items(folders) { f ->
                TextButton(onClick = { linkFolder(f) }) { Text(f.name) } } }
        } else LazyColumn { items(photos) { p ->
            AsyncImage(p.thumbnailLink, p.name,
                Modifier.fillMaxWidth().height(200.dp).padding(vertical = 4.dp)) } }
    }
}
