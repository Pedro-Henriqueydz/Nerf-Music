package com.nerf.music.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utilitário de criptografia autenticada utilizando o Android KeyStore (AES-256 GCM).
 * Protege credenciais de autenticação (como cookies do YouTube Music e tokens do Spotify)
 * contra extração não autorizada, backups em texto claro e inspeção do armazenamento local.
 */
object SecureStorageHelper {
    private const val TAG = "SecureStorageHelper"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "nerf_credentials_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SEPARATOR = "]"

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Criptografa o texto plano usando AES-256 GCM com vetor de inicialização (IV) aleatório.
     * Retorna a representação formatada "ivBase64]cipherBase64".
     */
    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrEmpty()) return plainText
        return try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            "$ivBase64$IV_SEPARATOR$cipherBase64"
        } catch (e: Exception) {
            Log.e(TAG, "Falha na criptografia de credencial: ${e.message}", e)
            plainText
        }
    }

    /**
     * Decriptografa o texto cifrado gerado por [encrypt].
     * Suporta compatibilidade regressiva: se a string não contiver o separador do IV,
     * é tratada como credencial legada em texto claro.
     */
    fun decrypt(cipherTextWithIv: String?): String? {
        if (cipherTextWithIv.isNullOrEmpty()) return cipherTextWithIv
        if (!cipherTextWithIv.contains(IV_SEPARATOR)) {
            // Dado legado em texto claro: retorna diretamente
            return cipherTextWithIv
        }
        return try {
            val parts = cipherTextWithIv.split(IV_SEPARATOR, limit = 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Falha na decriptografia de credencial: ${e.message}", e)
            cipherTextWithIv
        }
    }
}
