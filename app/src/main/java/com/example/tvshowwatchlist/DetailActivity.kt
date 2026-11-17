package com.example.tvshowwatchlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tvshowwatchlist.data.api.TvMazeApiService
import com.example.tvshowwatchlist.data.local.AppDatabase
import com.example.tvshowwatchlist.data.local.WatchlistItem
import com.example.tvshowwatchlist.data.model.TvShow
import com.example.tvshowwatchlist.data.repository.TvRepository
import com.example.tvshowwatchlist.databinding.ActivityDetailBinding
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var repository: TvRepository

    private var showId: Int = -1
    private var currentShow: TvShow? = null
    private var existingWatchlistItem: WatchlistItem? = null

    companion object {
        private const val EXTRA_SHOW_ID = "extra_show_id"

        fun start(context: Context, showId: Int) {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_SHOW_ID, showId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val db = AppDatabase.getDatabase(this)
        val api = TvMazeApiService.create()
        repository = TvRepository(api, db.watchlistDao())

        showId = intent.getIntExtra(EXTRA_SHOW_ID, -1)
        if (showId == -1) {
            Toast.makeText(this, "Invalid Show ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadShowDetails()
        setupListeners()
    }

    private fun loadShowDetails() {
        binding.pbDetail.visibility = View.VISIBLE
        binding.scrollViewDetail.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val show = repository.getShowDetails(showId)
                currentShow = show
                bindShowData(show)
                checkWatchlistState()
                binding.pbDetail.visibility = View.GONE
                binding.scrollViewDetail.visibility = View.VISIBLE
            } catch (e: Exception) {
                // If offline or network error, fallback to saved Room data if present
                existingWatchlistItem = repository.getWatchlistItemById(showId)
                binding.pbDetail.visibility = View.GONE
                if (existingWatchlistItem != null) {
                    bindWatchlistItemData(existingWatchlistItem!!)
                    updateWatchlistUiState(existingWatchlistItem)
                    binding.scrollViewDetail.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@DetailActivity, "Error loading show details: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bindShowData(show: TvShow) {
        binding.tvTitle.text = show.name
        binding.tvRating.text = show.rating?.average?.let { "⭐ %.1f / 10".format(it) } ?: "⭐ N/A"
        binding.tvLanguage.text = "Language: ${show.language ?: "N/A"}"
        binding.tvPremiered.text = "Premiered: ${show.premiered ?: "N/A"}"
        binding.tvGenres.text = show.genres?.joinToString(", ") ?: "N/A"

        // Remove HTML tags from summary
        val rawSummary = show.summary ?: "No summary available."
        val cleanedSummary = HtmlCompat.fromHtml(rawSummary, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        binding.tvSummary.text = cleanedSummary

        val imageUrl = show.image?.medium ?: show.image?.original
        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .into(binding.ivPoster)
    }

    private fun bindWatchlistItemData(item: WatchlistItem) {
        binding.tvTitle.text = item.name
        binding.tvRating.text = item.rating?.let { "⭐ %.1f / 10".format(it) } ?: "⭐ N/A"
        binding.tvLanguage.text = "Language: ${item.language ?: "N/A"}"
        binding.tvPremiered.text = "Premiered: ${item.premiered ?: "N/A"}"
        binding.tvGenres.text = item.genres ?: "N/A"
        binding.tvSummary.text = item.summary ?: "No summary available."

        Glide.with(this)
            .load(item.posterUrl)
            .centerCrop()
            .into(binding.ivPoster)
    }

    private suspend fun checkWatchlistState() {
        existingWatchlistItem = repository.getWatchlistItemById(showId)
        updateWatchlistUiState(existingWatchlistItem)
    }

    private fun updateWatchlistUiState(item: WatchlistItem?) {
        if (item != null) {
            binding.btnSaveWatchlist.text = "Update Watchlist"
            binding.btnRemoveWatchlist.visibility = View.VISIBLE
            when (item.status) {
                "Watching" -> binding.rbWatching.isChecked = true
                "Completed" -> binding.rbCompleted.isChecked = true
                else -> binding.rbPlanToWatch.isChecked = true
            }
        } else {
            binding.btnSaveWatchlist.text = getString(R.string.add_to_watchlist)
            binding.btnRemoveWatchlist.visibility = View.GONE
            binding.rbPlanToWatch.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.btnSaveWatchlist.setOnClickListener {
            val selectedStatus = when (binding.rgStatus.checkedRadioButtonId) {
                R.id.rbWatching -> "Watching"
                R.id.rbCompleted -> "Completed"
                else -> "Plan to Watch"
            }

            lifecycleScope.launch {
                val show = currentShow
                val itemToSave = if (show != null) {
                    val rawSummary = show.summary ?: ""
                    val cleanSummary = HtmlCompat.fromHtml(rawSummary, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
                    WatchlistItem(
                        id = show.id,
                        name = show.name,
                        posterUrl = show.image?.medium ?: show.image?.original,
                        rating = show.rating?.average,
                        summary = cleanSummary,
                        genres = show.genres?.joinToString(", "),
                        language = show.language,
                        premiered = show.premiered,
                        status = selectedStatus
                    )
                } else if (existingWatchlistItem != null) {
                    existingWatchlistItem!!.copy(status = selectedStatus, timestamp = System.currentTimeMillis())
                } else null

                if (itemToSave != null) {
                    repository.saveToWatchlist(itemToSave)
                    existingWatchlistItem = itemToSave
                    updateWatchlistUiState(itemToSave)
                    Toast.makeText(this@DetailActivity, "Saved to Watchlist ($selectedStatus)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnRemoveWatchlist.setOnClickListener {
            lifecycleScope.launch {
                repository.removeFromWatchlistById(showId)
                existingWatchlistItem = null
                updateWatchlistUiState(null)
                Toast.makeText(this@DetailActivity, "Removed from Watchlist", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
