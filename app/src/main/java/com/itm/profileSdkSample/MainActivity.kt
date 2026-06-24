package com.itm.profileSdkSample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.itm.profileSdkSample.databinding.ActivityMainBinding
import com.itm.profileSdkSample.databinding.ItemProfileRowBinding
import com.itm.profile_sdk.core.ISDKClient
import com.itm.profile_sdk.models.ScreenTimeEntry
import com.itm.profile_sdk.models.ScreenTimeRequest
import com.itm.profile_sdk.models.UpdateProfileRequest
import com.itm.profile_sdk.models.UserProfile
import com.itm.profile_sdk.util.Cancellable
import com.itm.profile_sdk.util.Result
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private data class Profile(val userId: String, val label: String)

    private lateinit var binding: ActivityMainBinding

    // ── Constants ─────────────────────────────────────────────────────────────

    private val internalKey = "6bebaa8645a54b27b858d74cbf5a1aac9f56b4945d85aa2f7abf0885df540c5b"

    // ── State ─────────────────────────────────────────────────────────────────

    private var currentToken: String? = null
    private var currentProfile: UserProfile? = null
    private var activeObserver: Cancellable? = null


    private val profiles = listOf(
        Profile(userId = "mMCb2xH89eUWqnJPOkV0WfGf2XO2", label = "Profile 1"),
        Profile(userId = "TbyvkxSKuBSNSRUdwv9uDfhbCN12", label = "Profile 2"),
        Profile(userId = "xPVwfiBkHSX6qBqnufSUssJdDWI2", label = "Profile 3"),
        Profile(userId = "wFpRlxosKPXKzjjDqwXpXVicoLc2", label = "Profile 4"),
        Profile(userId = "fbYd1XPRCTUPzV4wAgm9iysJq0A2", label = "Profile 5"),
        Profile(userId = "hM8rLApnpSRVYI1jVOdvnA71wJZ2", label = "Profile 6"),
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

        binding.btnUpdateProfile.setOnClickListener { updateProfile() }
        binding.btnAddScreenTime.setOnClickListener { addScreenTime() }
        binding.btnLoadProfileViews.setOnClickListener { loadProfileViews() }
        binding.btnLoadNearbyUsers.setOnClickListener { loadNearbyUsers() }

    }

    // ── SDK calls ─────────────────────────────────────────────────────────────

    private fun loadProfile(profile: Profile) {
        // Cancel any existing observer before switching users
        activeObserver?.cancel()
        currentToken = null
        currentProfile = null

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
                    currentToken = tokenResult.data
                    showLoading("Loading ${profile.label}...")

                    // 3. Observe profile — emits cached data instantly, refreshes in background
                    activeObserver = ISDKClient.observeProfile(
                        token = tokenResult.data,
                        onEach = { userProfile ->
                            currentProfile = userProfile
                            showProfile(userProfile)
                            loadScreenTime(tokenResult.data)
                        },
                        onError = { e -> showError("Profile error: ${e.message}") }
                    )
                }
            }
        }
    }
// ── Screen Time ───────────────────────────────────────────────────────────

    private fun loadScreenTime(token: String) {
        activeObserver = ISDKClient.observeScreenTime(
            token = token,
            7,
            onEach = { entries -> showScreenTime(entries) },
            onError = { e -> showError("Screen time error: ${e.message}") }
        )
    }

    private fun addScreenTime() {
        val token = currentToken ?: return showError("No token available")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        ISDKClient.postScreenTime(
            token = token,
            request = ScreenTimeRequest(date = today, seconds = 60)
        ) { result ->
            when (result) {
                is Result.Success -> showToast("✅ Added 60s screen time for $today")
                is Result.Error -> showError("Screen time error: ${result.message}")
                is Result.Loading -> { /* no-op */
                }
            }
        }
    }

    // ── Profile Views ─────────────────────────────────────────────────────────

    private fun loadProfileViews() {
        val token = currentToken ?: return showError("No token available")

        showToast("Loading profile views...")

        ISDKClient.getProfileViews(token = token) { result ->
            when (result) {
                is Result.Success -> {
                    val data = result.data
                    val sb = StringBuilder()
                    sb.appendLine("Total views: ${data.total ?: 0}")
                    sb.appendLine()
                    if (data.items.isNullOrEmpty()) {
                        sb.append("No viewers yet.")
                    } else {
                        data.items?.forEachIndexed { index, viewer ->
                            sb.appendLine("${index + 1}. ${viewer.name ?: "Unknown"}")
                            sb.appendLine("   Viewed ${viewer.viewCount ?: 0}x")
                            sb.appendLine("   Last: ${viewer.lastViewedAt ?: "—"}")
                            sb.appendLine("   Country: ${viewer.country ?: "—"}")
                            if (index < (data.items?.size ?: (0 - 1))) sb.appendLine()
                        }
                    }
                    runOnUiThread {
                        binding.tvProfileViews.text = sb.toString()
                        binding.cardProfileViews.visibility = View.VISIBLE
                    }
                }

                is Result.Error -> showError("Profile views error: ${result.message}")
                is Result.Loading -> { /* no-op */
                }
            }
        }
    }

    // ── Update Profile ────────────────────────────────────────────────────────

    private fun updateProfile() {
        val token = currentToken ?: return showError("No token available")
        val profile = currentProfile ?: return showError("No profile loaded")

        // Demo: append "(Updated)" to the current name
        val newName = if (profile.name?.contains("(Updated)") == true) {
            profile.name?.replace(" (Updated)", "")
        } else {
            "${profile.name} (Updated)"
        }

        val newDob = if (profile.dob?.equals("1994-09-04") == true) {
            profile.dob?.replace(" 1994-09-04", "1994-09-03")
        } else {
            "${profile.dob} 1994-09-04"
        }

        showLoading("Updating profile...")

        ISDKClient.updateProfile(
            token = token,
            request = UpdateProfileRequest(name = newName, dob = "19289/292/292")
        ) { result ->
            when (result) {
                is Result.Success -> showToast("✅ Profile updated!")
                is Result.Error -> showError("Update failed: ${result.message}")
                is Result.Loading -> { /* no-op */
                }
            }
        }
    }


    // ── Nearby Users ──────────────────────────────────────────────────────────

    private fun loadNearbyUsers() {
        val token = currentToken ?: return showError("No token available")

        showToast("Loading nearby users...")

        ISDKClient.getNearbyUsers(
            token = token, lat = 24.8607,
            lng = 67.0011
        ) { result ->
            when (result) {
                is Result.Success -> {
                    val users = result.data
                    val sb = StringBuilder()
                    if (users.isEmpty()) {
                        sb.append("No nearby users found.")
                    } else {
                        sb.appendLine("Found ${users.size} nearby user(s):")
                        sb.appendLine()
                        users.forEachIndexed { index, user ->
                            sb.appendLine("${index + 1}. ${user.name ?: "Unknown"}")
                            sb.appendLine("   Gender: ${user.gender ?: "—"}")
                            sb.appendLine("   Visibility: ${user.visibility ?: "—"}")
                            user.location?.let {
                                sb.appendLine("   📍 ${it.lat}, ${it.lng}")
                            }
                            if (index < users.size - 1) sb.appendLine()
                        }
                    }
                    runOnUiThread {
                        binding.tvNearbyUsers.text = sb.toString().trimEnd()
                        binding.cardNearbyUsers.visibility = View.VISIBLE
                    }
                }

                is Result.Error -> showError("Nearby users error: ${result.message}")
                is Result.Loading -> { /* no-op */
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
            binding.cardScreenTime.visibility = View.VISIBLE

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

    private fun showScreenTime(entries: List<ScreenTimeEntry>) {
        runOnUiThread {
            val sb = StringBuilder()
            if (entries.isEmpty()) {
                sb.append("No screen time data yet.")
            } else {
                entries.forEach { entry ->
                    sb.appendLine("📅 ${entry.date ?: "—"}  →  ${entry.minutes ?: 0} min")
                }
            }
            binding.tvScreenTime.text = sb.toString().trimEnd()
        }
    }


    private fun setRow(row: ItemProfileRowBinding, label: String, value: String) {
        row.tvLabel.text = label
        row.tvValue.text = value
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}