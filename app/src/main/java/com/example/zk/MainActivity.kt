package com.example.zk

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import com.example.zk.navigation.AppNav
import com.example.zk.ui.theme.ZKTheme
import com.example.zk.viewmodel.PassportViewModel

private const val TAG = "MainActivity"

// CompositionLocal to provide PassportViewModel to the entire app
val LocalPassportViewModel = staticCompositionLocalOf<PassportViewModel> {
    error("No PassportViewModel provided")
}

class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private lateinit var nfcIntentFilters: Array<IntentFilter>
    private lateinit var nfcTechLists: Array<Array<String>>

    // Use activity-scoped ViewModel
    private val passportViewModel: PassportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Log.e(TAG, "NFC is not available on this device")
            Toast.makeText(this, "NFC not available on this device", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
            Log.w(TAG, "NFC is disabled")
            Toast.makeText(this, "Please enable NFC in settings", Toast.LENGTH_LONG).show()
        } else {
            Log.d(TAG, "NFC is available and enabled")
        }

        // Create pending intent for NFC
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        // Explicit IntentFilter + tech list so foreground dispatch catches
        // IsoDep (passport) tags reliably on all OEMs (Honor, Huawei, etc.)
        nfcIntentFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        nfcTechLists = arrayOf(
            arrayOf(IsoDep::class.java.name)
        )

        // Handle NFC intent that may have launched / re-launched the activity
        handleNfcIntent(intent)

        setContent {
            ZKTheme {
                // Provide the PassportViewModel to the entire composition
                CompositionLocalProvider(LocalPassportViewModel provides passportViewModel) {
                    AppNav()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable NFC foreground dispatch with explicit tech filters
        Log.d(TAG, "Enabling NFC foreground dispatch")
        nfcAdapter?.enableForegroundDispatch(
            this, pendingIntent, nfcIntentFilters, nfcTechLists
        )
    }

    override fun onPause() {
        super.onPause()
        // Disable NFC foreground dispatch
        Log.d(TAG, "Disabling NFC foreground dispatch")
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // update the stored intent
        Log.d(TAG, "onNewIntent received: action=${intent.action}")
        handleNfcIntent(intent)
    }

    /**
     * Extract NFC tag from any intent and forward to the ViewModel.
     * Called from both onCreate (cold-start / relaunch) and onNewIntent.
     */
    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return

        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {

            Log.d(TAG, "NFC action detected! (${intent.action})")

            val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            if (tag != null) {
                Log.d(TAG, "NFC Tag found: $tag")
                Toast.makeText(this, "Passport detected!", Toast.LENGTH_SHORT).show()
                passportViewModel.handleNfcTag(tag)
            } else {
                Log.e(TAG, "NFC Tag is null in intent")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ZKTheme {
        Greeting("Android")
    }
}