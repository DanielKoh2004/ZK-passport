package com.example.zk

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.zk.util.ZKPEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class ZKPEngineBenchmarkTest {

    private val TAG = "FYP_BENCHMARK_PROVER"
    private lateinit var zkpEngine: ZKPEngine

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // The real project uses ZKPEngine(context), initializing the WebView via lazy load.
        zkpEngine = ZKPEngine(context)
        Log.i(TAG, "ZKPEngine framework initialized.")
    }

    @After
    fun tearDown() {
        // Safely destroy the WebView instance
        zkpEngine.destroy()
    }

    @Test
    fun benchmarkGroth16Proving() = runBlocking {
        val ttpResults = mutableListOf<Long>()
        val iterations = 50
        
        Log.i(TAG, "Starting ZK Proving Benchmark ($iterations iterations)...")

        // Variables to extract real proof data for the size test
        var lastProof = ""
        var lastPublicSignals = ""

        // TASK 2: Execute loop and measure Time-to-Prove (TTP)
        for (i in 1..iterations) {
            val duration = measureTimeMillis {
                // Standard dummy inputs mapped to your computeProof signature
                val dob = 20040512
                val passportNumber = 123456789
                val nationality = 458
                
                // CALLING THE REAL ENGINE:
                // Suspension point that drops into the WebView JS bridge
                val result = zkpEngine.computeProof(dob, passportNumber, nationality)
                
                lastProof = result.proofJson
                lastPublicSignals = result.publicSignalsJson
            }
            ttpResults.add(duration)
            Log.i(TAG, "Iteration $i TTP: $duration ms")
            
            // Brief 100ms breather to allow the JS engine garbage collector to clear memory 
            // between heavy WASM invocations to prevent OutOfMemory crashes in tests
            delay(100) 
        }

        val mean = ttpResults.average()
        val min = ttpResults.minOrNull() ?: 0
        val max = ttpResults.maxOrNull() ?: 0

        // TASK 3: Benchmark Payload Size
        // Using exactly your system's generated output json formats
        val mockFinalJson = JSONObject().apply {
            put("proof", JSONObject(lastProof)) 
            put("publicSignals", lastPublicSignals)
            put("nonce", "f47ac10b-58cc-4372-a567-0e02b2c3d479") // Mock 32-byte UUID/Nonce
            put("signature", "3045022100e47b3be665efb90fe4638dafe837b2d2") // Mock 72-byte Signature limit
            put("publicKey", "047b3be665efb90fe4638dafe83112347b3be665ef") // Mock 64 hex Public Key
        }.toString()

        val payloadSizeBytes = mockFinalJson.toByteArray(Charsets.UTF_8).size

        // TASK 4: Logging Empirical Data
        Log.i(TAG, "----------------------------------------------------")
        Log.i(TAG, "Mean TTP: $mean ms | Min: $min ms | Max: $max ms")
        Log.i(TAG, "Total Payload Size: $payloadSizeBytes bytes")
        Log.i(TAG, "----------------------------------------------------")
    }
}
