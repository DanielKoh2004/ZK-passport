package com.example.zk.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ── Request / Response DTOs ─────────────────────────────────────────────────

data class IssuePassportRequest(
    val did: String,
    val name: String,
    val dateOfBirth: Int,
    val passportNumber: Int,
    val nationality: Int
)

data class PublicKeyResponse(
    val algorithm: String,
    val publicKey: String,
    val format: String
)

/** Wraps the signed Verifiable Credential returned by the issuer. */
data class IssuePassportResponse(
    val verifiableCredential: VerifiableCredential
)

data class VerifiableCredential(
    val type: List<String>,
    val issuer: VcIssuer,
    val issuanceDate: String,
    val credentialSubject: CredentialSubject,
    val proof: VcProof
)

data class VcIssuer(val id: String, val name: String)

data class CredentialSubject(
    val id: String,
    val name: String,
    val dateOfBirth: Int,
    val passportNumber: Int,
    val nationality: Int
)

data class VcProof(
    val type: String,
    val created: String,
    val verificationMethod: String,
    val proofPurpose: String,
    val proofValue: String
)

// ── Retrofit Interface ──────────────────────────────────────────────────────

interface IssuerApi {

    @GET("api/public-key")
    suspend fun getPublicKey(): PublicKeyResponse

    @POST("api/issue-passport")
    suspend fun issuePassport(@Body request: IssuePassportRequest): IssuePassportResponse
}

// ── Singleton Retrofit Instance ─────────────────────────────────────────────

object IssuerApiClient {

    /**
     * 10.0.2.2 is the Android emulator's alias for the host machine's localhost.
     * Change to your machine's LAN IP if testing on a physical device.
     */
    private const val BASE_URL = "http://10.0.2.2:3000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: IssuerApi = retrofit.create(IssuerApi::class.java)
}
