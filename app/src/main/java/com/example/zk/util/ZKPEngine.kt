package com.example.zk.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ZKPEngine
 *
 * Uses a hidden Android WebView to execute snarkjs Groth16 proof generation
 * on‐device. The WebView loads passport.wasm + passport_final.zkey from the
 * assets folder and calls back via a JavascriptInterface.
 */
class ZKPEngine(private val context: Context) {

    companion object {
        private const val TAG = "ZKPEngine"
        private const val PROVER_URL = "file:///android_asset/prover.html"
    }

    private var webView: WebView? = null
    private var isPageLoaded = false

    // ── Initialise the hidden WebView (must be called on Main thread) ───────
    @SuppressLint("SetJavaScriptEnabled")
    fun init(bridge: WebAppInterface) {
        Log.d(TAG, "Initialising hidden WebView …")
        val wv = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = true
            settings.domStorageEnabled = true

            addJavascriptInterface(bridge, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d(TAG, "prover.html loaded (onPageFinished)")
                    isPageLoaded = true
                }
            }

            // Forward console.log to Logcat for debugging
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(
                    message: String?, lineNumber: Int, sourceID: String?
                ) {
                    Log.d(TAG, "[WebView console] $message  ($sourceID:$lineNumber)")
                }
            }
        }

        wv.loadUrl(PROVER_URL)
        webView = wv
        Log.d(TAG, "WebView created, loading $PROVER_URL")
    }

    // ── Public suspend function: compute a Groth16 proof ────────────────────

    /**
     * Generates a ZK proof with the given passport integers.
     *
     * @param dob              dateOfBirth   as YYYYMMDD int  (e.g. 20040512)
     * @param passportNumber   passport #    as int
     * @param nationality      nationality   as int
     * @param ageThreshold     currentAgeThreshold as YYYYMMDD int (e.g. 20080101)
     * @return [ProofResult] containing proof JSON and publicSignals JSON
     */
    suspend fun computeProof(
        dob: Int,
        passportNumber: Int,
        nationality: Int,
        ageThreshold: Int = defaultAgeThreshold()
    ): ProofResult = suspendCancellableCoroutine { cont ->

        Log.d(TAG, "computeProof() called – dob=$dob, passport#=$passportNumber, nat=$nationality, threshold=$ageThreshold")

        val bridge = object : WebAppInterface() {
            @JavascriptInterface
            override fun onProofSuccess(proofJson: String, publicSignalsJson: String) {
                Log.d(TAG, "onProofSuccess received from JS")
                Log.d(TAG, "  proof:         ${proofJson.take(120)}…")
                Log.d(TAG, "  publicSignals: $publicSignalsJson")

                if (cont.isActive) {
                    cont.resume(ProofResult(proofJson, publicSignalsJson))
                }
            }

            @JavascriptInterface
            override fun onProofError(error: String) {
                Log.e(TAG, "onProofError received from JS: $error")
                if (cont.isActive) {
                    cont.resumeWithException(RuntimeException("ZK proof generation failed: $error"))
                }
            }
        }

        // WebView methods MUST run on the main thread
        Handler(Looper.getMainLooper()).post {
            try {
                // Lazily init the WebView on first call
                if (webView == null) {
                    init(bridge)
                } else {
                    webView!!.removeJavascriptInterface("AndroidBridge")
                    webView!!.addJavascriptInterface(bridge, "AndroidBridge")
                }

                val inputJson = """{"dateOfBirth":"$dob","passportNumber":"$passportNumber","nationality":"$nationality","currentAgeThreshold":"$ageThreshold"}"""

                Log.d(TAG, "Injecting JS call with input: $inputJson")

                val jsCall = "generateProof('${inputJson.replace("'", "\\'")}')"

                // If page is already loaded, invoke immediately; otherwise wait
                if (isPageLoaded) {
                    webView!!.evaluateJavascript(jsCall, null)
                } else {
                    webView!!.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            Log.d(TAG, "prover.html now loaded – invoking generateProof")
                            isPageLoaded = true
                            webView!!.evaluateJavascript(jsCall, null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error posting JS call", e)
                if (cont.isActive) cont.resumeWithException(e)
            }
        }

        cont.invokeOnCancellation {
            Log.w(TAG, "computeProof coroutine cancelled")
        }
    }

    /** Clean up WebView resources. */
    fun destroy() {
        webView?.destroy()
        webView = null
        isPageLoaded = false
        Log.d(TAG, "WebView destroyed")
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Default age threshold = 18 years ago from today as YYYYMMDD. */
    private fun defaultAgeThreshold(): Int {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR) - 18
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return y * 10000 + m * 100 + d
    }

    // ── Bridge interface & result data class ────────────────────────────────

    abstract class WebAppInterface {
        @JavascriptInterface
        abstract fun onProofSuccess(proofJson: String, publicSignalsJson: String)

        @JavascriptInterface
        abstract fun onProofError(error: String)
    }

    data class ProofResult(
        val proofJson: String,
        val publicSignalsJson: String
    )
}
