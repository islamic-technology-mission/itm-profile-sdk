package com.itm.profileSdkSample

import android.net.http.HttpResponseCache.install
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.itm.profileSdkSample.databinding.ActivityMainBinding
import com.itm.profileSdkSample.databinding.ItemProfileRowBinding
import com.itm.profile_sdk.core.ISDKClient
import com.itm.profile_sdk.models.UserProfile
import com.itm.profile_sdk.util.Result

class MainActivity : AppCompatActivity() {
    private data class Profile(val userId: String, val label: String)

    private lateinit var binding: ActivityMainBinding

    // ── Constants ─────────────────────────────────────────────────────────────

    private val internalKey = "6bebaa8645a54b27b858d74cbf5a1aac9f56b4945d85aa2f7abf0885df540c5b"

    private val profiles = listOf(
        Profile(userId = "mMCb2xH89eUWqnJPOkV0WfGf2XO2", label = "Profile 1"),
        Profile(userId = "TbyvkxSKuBSNSRUdwv9uDfhbCN12", label = "Profile 2"),
        Profile(userId = "xPVwfiBkHSX6qBqnufSUssJdDWI2", label = "Profile 3"),
        Profile(userId = "wFpRlxosKPXKzjjDqwXpXVicoLc2", label = "Profile 4"),
    )


    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupButtons()


    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnProfile1.setOnClickListener { loadProfile(profiles[0]) }
        binding.btnProfile2.setOnClickListener { loadProfile(profiles[1]) }
        binding.btnProfile3.setOnClickListener { loadProfile(profiles[2]) }
        binding.btnProfile4.setOnClickListener { loadProfile(profiles[3]) }
    }

    // ── SDK calls ─────────────────────────────────────────────────────────────

    private fun loadProfile(profile: Profile) {
        showLoading("Generating token for ${profile.label}...")

        // 1. Re-initialize SDK with selected userId
        ISDKClient.initialize(
            userId = profile.userId,
            context = applicationContext
        )

        // 2. Generate token
        ISDKClient.generateToken(internalKey = internalKey) { tokenResult ->
            when (tokenResult) {
                is Result.Error -> showError("Token error: ${tokenResult.message}")
                is Result.Loading -> { /* no-op */
                }

                is Result.Success -> {
                    val token = tokenResult.data
                    showLoading("Loading ${profile.label}...")
                    // 3. Observe profile — emits cached data instantly, refreshes in background
                    ISDKClient.observeProfile(
                        token = token,
                        onEach = { userProfile -> showProfile(userProfile) },
                        onError = { e -> showError("Profile error: ${e.message}") }
                    )
                }
            }
        }
    }

    // ── UI State ──────────────────────────────────────────────────────────────

    private fun showLoading(message: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.VISIBLE
            binding.cardProfile.visibility = View.GONE
            binding.tvStatus.visibility = View.VISIBLE
            binding.tvStatus.text = message
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            binding.tvStatus.text = "❌ $message"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showProfile(profile: UserProfile) {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            binding.tvStatus.visibility = View.GONE
            binding.cardProfile.visibility = View.VISIBLE

            binding.tvName.text = profile.name ?: "—"
            binding.tvEmail.text = profile.email ?: "—"

            setRow(binding.rowPhone, "Phone", profile.phone?.ifBlank { "—" } ?: "—")
            setRow(binding.rowGender, "Gender", profile.gender ?: "—")
            setRow(binding.rowDob, "DOB", profile.dob ?: "—")
            setRow(binding.rowNearby, "Nearby", "${profile.nearbyUsers ?: 0} users")
            setRow(binding.rowJoined, "Joined", profile.createdAt ?: "—")
            setRow(
                binding.rowLocation, "Location",
                profile.location?.let { "${it.lat}, ${it.lng}" } ?: "—"
            )
        }
    }

    private fun setRow(row: ItemProfileRowBinding, label: String, value: String) {
        row.tvLabel.text = label
        row.tvValue.text = value
    }
}