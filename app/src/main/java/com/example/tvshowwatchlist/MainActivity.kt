package com.example.tvshowwatchlist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tvshowwatchlist.data.api.TvMazeApiService
import com.example.tvshowwatchlist.data.local.AppDatabase
import com.example.tvshowwatchlist.data.local.WatchlistItem
import com.example.tvshowwatchlist.data.repository.TvRepository
import com.example.tvshowwatchlist.databinding.ActivityMainBinding
import com.example.tvshowwatchlist.ui.adapter.ShowAdapter
import com.example.tvshowwatchlist.ui.adapter.TextLineAdapter
import com.example.tvshowwatchlist.ui.adapter.WatchlistAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: TvRepository

    private lateinit var showAdapter: ShowAdapter
    private lateinit var watchlistAdapter: WatchlistAdapter
    private lateinit var textLineAdapter: TextLineAdapter

    private var firebaseAuth: FirebaseAuth? = null
    private var googleSignInClient: GoogleSignInClient? = null
    private var currentGoogleAccount: com.google.android.gms.auth.api.signin.GoogleSignInAccount? = null

    private val allowedEmails = setOf(
        "anupojuramesh8994@gmail.com",
        "231801380024@cutmap.ac.in",
        "bowthyasreeanupoju@gmail.com",
        "itsmemyownworld@gmail.com",
        "useranna2005@gmail.com"
    )

    private var currentFilterStatus: String = "All"

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val userEmail = account?.email?.lowercase()?.trim()

                if (userEmail != null && allowedEmails.contains(userEmail)) {
                    currentGoogleAccount = account
                    updateAuthUserUi()
                    Toast.makeText(this, "Signed in as ${account.displayName ?: userEmail}", Toast.LENGTH_SHORT).show()

                    val idToken = account.idToken
                    if (idToken != null && firebaseAuth != null) {
                        firebaseSignInWithGoogle(idToken)
                    }
                } else if (userEmail != null) {
                    googleSignInClient?.signOut()
                    currentGoogleAccount = null
                    updateAuthUserUi()
                    showAuthErrorDialog(
                        "Access Denied",
                        "The email address '$userEmail' is not authorized to access this app.\n\n" +
                        "Only authorized accounts are granted access."
                    )
                } else {
                    Toast.makeText(this, "Could not retrieve account details.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                if (e.statusCode != 12501) { // 12501 is user cancelled
                    val message = when (e.statusCode) {
                        12500 -> "SIGN_IN_FAILED (Code 12500):\nGoogle Play Services failed to sign in. Please check Google Play Services on your device."
                        4 -> "SIGN_IN_REQUIRED (Code 4):\nPlease try signing in again."
                        else -> "Sign-In Error (Code ${e.statusCode}): ${e.localizedMessage}"
                    }
                    showAuthErrorDialog("Google Sign-In Error", message)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Sign-In Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

    private var photoUri: Uri? = null

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to scan posters", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    @Suppress("DEPRECATION")
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    binding.ivScanPreview.setImageBitmap(bitmap)
                    processImageWithMlKit(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoUri?.let { uri ->
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        }
                        binding.ivScanPreview.setImageBitmap(bitmap)
                        processImageWithMlKit(bitmap)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to load captured image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                binding.ivScanPreview.setImageBitmap(it)
                processImageWithMlKit(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRepository()
        setupAuth()
        setupAdapters()
        setupTabs()
        setupSearch()
        setupScanPoster()
        setupWatchlist()

        // Initial default search
        val initialQuery = "The Flash"
        binding.etSearchQuery.setText(initialQuery)
        performSearch(initialQuery)
    }

    private fun setupRepository() {
        val db = AppDatabase.getDatabase(this)
        val api = TvMazeApiService.create()
        repository = TvRepository(api, db.watchlistDao())
    }

    private fun setupAuth() {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
        } catch (_: Exception) {
            // Firebase Auth not initialized or available
        }

        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        val webClientId = getString(R.string.default_web_client_id)
        if (webClientId.isNotEmpty() && !webClientId.contains("YOUR_DEFAULT_WEB_CLIENT_ID")) {
            gsoBuilder.requestIdToken(webClientId)
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())

        // Check if user was previously signed in with an authorized Google account
        val lastAccount = GoogleSignIn.getLastSignedInAccount(this)
        val lastEmail = lastAccount?.email?.lowercase()?.trim()
        if (lastEmail != null && allowedEmails.contains(lastEmail)) {
            currentGoogleAccount = lastAccount
        } else if (lastAccount != null) {
            googleSignInClient?.signOut()
            currentGoogleAccount = null
        }

        updateAuthUserUi()

        binding.btnAuth.setOnClickListener {
            val signedInAccount = currentGoogleAccount ?: GoogleSignIn.getLastSignedInAccount(this)?.takeIf {
                it.email?.lowercase()?.trim() in allowedEmails
            }
            val firebaseUser = firebaseAuth?.currentUser

            if (signedInAccount != null || firebaseUser != null) {
                currentGoogleAccount = null
                firebaseAuth?.signOut()
                googleSignInClient?.signOut()?.addOnCompleteListener {
                    updateAuthUserUi()
                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (googleSignInClient != null) {
                    val signInIntent = googleSignInClient!!.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                } else {
                    Toast.makeText(this, "Google Sign-In client unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun firebaseSignInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    updateAuthUserUi()
                }
            }
    }

    private fun showAuthErrorDialog(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateAuthUserUi() {
        val googleAccount = currentGoogleAccount ?: GoogleSignIn.getLastSignedInAccount(this)?.takeIf {
            it.email?.lowercase()?.trim() in allowedEmails
        }
        val firebaseUser = firebaseAuth?.currentUser

        val userEmail = googleAccount?.email ?: firebaseUser?.email
        val userName = googleAccount?.displayName ?: firebaseUser?.displayName
        val userAvatar = googleAccount?.photoUrl ?: firebaseUser?.photoUrl

        if (userEmail != null && allowedEmails.contains(userEmail.lowercase().trim())) {
            binding.tvUserName.text = userName ?: "BOWTHYA SREE ANUPOJU"
            binding.tvUserEmail.text = userEmail
            binding.btnAuth.text = getString(R.string.sign_out)
            if (userAvatar != null) {
                Glide.with(this).load(userAvatar).circleCrop().into(binding.ivUserAvatar)
            } else {
                binding.ivUserAvatar.setImageResource(R.drawable.ic_user_avatar)
            }
        } else {
            binding.tvUserName.text = "BOWTHYA SREE ANUPOJU"
            binding.tvUserEmail.text = "231801380024@cutmap.ac.in"
            binding.btnAuth.text = getString(R.string.sign_out)
            binding.ivUserAvatar.setImageResource(R.drawable.ic_user_avatar)
        }
    }

    private fun setupAdapters() {
        showAdapter = ShowAdapter { show ->
            DetailActivity.start(this, show.id)
        }
        binding.rvSearchShows.layoutManager = GridLayoutManager(this, 2)
        binding.rvSearchShows.adapter = showAdapter

        watchlistAdapter = WatchlistAdapter { item ->
            DetailActivity.start(this, item.id)
        }
        binding.rvWatchlist.layoutManager = LinearLayoutManager(this)
        binding.rvWatchlist.adapter = watchlistAdapter

        textLineAdapter = TextLineAdapter { selectedLine ->
            binding.etSearchQuery.setText(selectedLine)
            binding.tabLayout.getTabAt(0)?.select()
            performSearch(selectedLine)
        }
        binding.rvTextLines.layoutManager = LinearLayoutManager(this)
        binding.rvTextLines.adapter = textLineAdapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Search Shows
                        binding.layoutSearch.visibility = View.VISIBLE
                        binding.layoutScan.visibility = View.GONE
                        binding.layoutWatchlist.visibility = View.GONE
                    }
                    1 -> { // Poster Scanner
                        binding.layoutSearch.visibility = View.GONE
                        binding.layoutScan.visibility = View.VISIBLE
                        binding.layoutWatchlist.visibility = View.GONE
                    }
                    2 -> { // My Watchlist
                        binding.layoutSearch.visibility = View.GONE
                        binding.layoutScan.visibility = View.GONE
                        binding.layoutWatchlist.visibility = View.VISIBLE
                        observeWatchlist()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearchQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(this, "Please enter a show name to search", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchQuery.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else false
        }
    }

    private fun performSearch(query: String) {
        binding.pbSearch.visibility = View.VISIBLE
        binding.tvEmptySearch.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val results = repository.searchShows(query)
                val shows = results.map { it.show }
                binding.pbSearch.visibility = View.GONE
                if (shows.isEmpty()) {
                    binding.tvEmptySearch.visibility = View.VISIBLE
                }
                showAdapter.submitList(shows)
            } catch (e: Exception) {
                binding.pbSearch.visibility = View.GONE
                binding.tvEmptySearch.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "Search error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupScanPoster() {
        binding.btnPickGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnTakePhoto.setOnClickListener {
            checkPermissionAndOpenCamera()
        }
    }

    private fun checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        try {
            val photoFile = File.createTempFile("poster_scan_", ".jpg", cacheDir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
            photoUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            try {
                cameraLauncher.launch(null)
            } catch (ex: Exception) {
                Toast.makeText(this, "Could not launch camera: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processImageWithMlKit(bitmap: Bitmap) {
        binding.pbScan.visibility = View.VISIBLE
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                binding.pbScan.visibility = View.GONE
                val lines = visionText.textBlocks
                    .flatMap { it.lines }
                    .map { it.text.trim() }
                    .filter { it.isNotEmpty() }

                if (lines.isEmpty()) {
                    Toast.makeText(this, "No text detected in poster image", Toast.LENGTH_SHORT).show()
                }
                textLineAdapter.submitList(lines)
            }
            .addOnFailureListener { e ->
                binding.pbScan.visibility = View.GONE
                Toast.makeText(this, "Text recognition failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupWatchlist() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterStatus = when {
                checkedIds.contains(R.id.chipFilterPlan) -> "Plan to Watch"
                checkedIds.contains(R.id.chipFilterWatching) -> "Watching"
                checkedIds.contains(R.id.chipFilterCompleted) -> "Completed"
                else -> "All"
            }
            observeWatchlist()
        }

        // Swipe-to-delete implementation
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val itemToDelete = watchlistAdapter.getItemAt(position)
                    lifecycleScope.launch {
                        repository.removeFromWatchlist(itemToDelete)
                        Snackbar.make(binding.root, "Deleted ${itemToDelete.name}", Snackbar.LENGTH_LONG)
                            .setAction("Undo") {
                                lifecycleScope.launch {
                                    repository.saveToWatchlist(itemToDelete)
                                }
                            }
                            .show()
                    }
                }
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvWatchlist)
    }

    private fun observeWatchlist() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val flow = if (currentFilterStatus == "All") {
                    repository.getAllWatchlist()
                } else {
                    repository.getWatchlistByStatus(currentFilterStatus)
                }

                flow.collect { items ->
                    if (items.isEmpty()) {
                        binding.tvEmptyWatchlist.visibility = View.VISIBLE
                    } else {
                        binding.tvEmptyWatchlist.visibility = View.GONE
                    }
                    watchlistAdapter.submitList(items)
                }
            }
        }
    }
}
