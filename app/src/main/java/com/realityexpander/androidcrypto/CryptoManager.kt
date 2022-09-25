package com.realityexpander.androidcrypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(Build.VERSION_CODES.M)
class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val encryptCipher
        get() = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }

    private fun getDecryptCipherForIv(iv: ByteArray): Cipher {
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
        }
    }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry("secret", null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        return KeyGenerator.getInstance(ALGORITHM).apply {
            init(
                KeyGenParameterSpec.Builder(
                    "secret",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    fun encrypt(bytes: ByteArray, outputStream: OutputStream): ByteArray {
        //val startPaddingForAES = byteArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
        //val startPaddingForAES = byteArrayOf(127,127,127,127,127,127,127,127,127,127,127,127,127,127,127,127)
        //val startPaddingForAES = byteArrayOf(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16)
        //val startPaddingForAES = encryptCipher.iv

//        var bytesToEncrypt = bytes.copyOf()
//        if (bytes.size + startPaddingForAES.size < 112) {  // 112 + 16 = 128
//            val additionalBytes = 112 - (bytes.size + startPaddingForAES.size)
//            bytesToEncrypt += ByteArray(additionalBytes)
//        }
//        val encryptedBytes = encryptCipher.doFinal(startPaddingForAES + bytesToEncrypt)

        val encryptedBytes = encryptCipher.doFinal(encryptCipher.iv + bytes)

        outputStream.use {
            it.write(encryptCipher.iv.size)
            it.write(encryptCipher.iv)
            it.write(encryptedBytes.size)
            it.write(encryptedBytes)
        }
        return encryptedBytes
    }

    fun decrypt(inputStream: InputStream): ByteArray {
        return inputStream.use {
            val ivSize = it.read()
            val iv = ByteArray(ivSize)
            it.read(iv)

            val encryptedBytesSize = it.read()
            val encryptedBytes = ByteArray(encryptedBytesSize)
            it.read(encryptedBytes)

            val decipheredByteArray = getDecryptCipherForIv(iv)
                .doFinal(encryptedBytes)
//            val decipheredIvLength = 128 - decipheredByteArray.size


            decipheredByteArray
                .toList()
                .subList(16, decipheredByteArray.size) // skip the IV
                .toByteArray()
                .decodeToString()
                .substringBefore(Char(0)) // trim the trailing zero characters
                .toByteArray()

//            getDecryptCipherForIv(iv)
//                .doFinal(encryptedBytes)
//                .toList()
//                .subList(16, encryptedBytes.size - 1) // skip the IV
//                .toByteArray()
//                .decodeToString()
//                .substringBefore(Char(0)) // trim the trailing zero characters
//                .toByteArray()
        }
    }

    companion object {
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC

                private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7 // uses variable block size
//        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE // block size must be fixed

        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    }

}