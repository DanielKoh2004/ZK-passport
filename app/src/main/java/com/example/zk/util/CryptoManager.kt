package com.example.zk.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

/**
 * CryptoManager — handles real Android KeyStore cryptography.
 *
 * • Generates / retrieves an EC (secp256r1 / PRIME256V1) KeyPair stored in the
 *   hardware-backed Android KeyStore.
 * • Derives a DID (Decentralized Identifier) from the public key.
 */
object CryptoManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "zk_wallet_device_key"

    /**
     * Get or create the device EC KeyPair from the Android KeyStore.
     * The private key never leaves the secure hardware.
     */
    fun getOrCreateKeyPair(): java.security.KeyPair {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        // If the key already exists, return it
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as java.security.PrivateKey
            val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey
            return java.security.KeyPair(publicKey, privateKey)
        }

        // Generate a new EC key pair inside the KeyStore
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
            .setUserAuthenticationRequired(false) // set true for biometric-gated access
            .build()

        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Return the public key bytes (X.509 / DER encoded).
     */
    fun getPublicKeyBytes(): ByteArray {
        val keyPair = getOrCreateKeyPair()
        return keyPair.public.encoded
    }

    /**
     * Derive a DID from the device's public key.
     * Format: did:device:<base64url_encoded_public_key>
     */
    fun getDeviceDid(): String {
        val pubKeyBytes = getPublicKeyBytes()
        val encoded = Base64.encodeToString(pubKeyBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "did:device:$encoded"
    }

    /**
     * Sign arbitrary data with the device private key (SHA256withECDSA).
     */
    fun sign(data: ByteArray): ByteArray {
        val keyPair = getOrCreateKeyPair()
        val signature = java.security.Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(data)
        return signature.sign()
    }

    /**
     * Verify a signature against the device's public key.
     */
    fun verify(data: ByteArray, signatureBytes: ByteArray): Boolean {
        val keyPair = getOrCreateKeyPair()
        val signature = java.security.Signature.getInstance("SHA256withECDSA")
        signature.initVerify(keyPair.public)
        signature.update(data)
        return signature.verify(signatureBytes)
    }
}
