/*
 *  NovaWallet, Cryptocurrency Wallet for Android
 *  Copyright (C) 2018 Alan Evans, NovaCrypto
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  Original source: https://github.com/NovaCrypto/NovaWallet
 *  You can contact the authors via github issues.
 */

package io.github.novacrypto.fingerprint

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import io.github.novacrypto.security.Base16
import kotlinx.android.synthetic.main.activity_finger_print_demo.*
import kotlinx.android.synthetic.main.content_finger_print_demo.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(Build.VERSION_CODES.M)

class FingerPrintDemoActivity : AppCompatActivity() {

    // val AUTHENTICATION_DURATION_SECONDS = 10

    private lateinit var fingerprintWrapper: FingerprintWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finger_print_demo)
        setSupportActionBar(toolbar)

        fingerprintWrapper = FingerprintWrapper(this)

        encode.isEnabled = fingerprintWrapper.canTakeFingerprint
        decode.isEnabled = fingerprintWrapper.canTakeFingerprint

        textEncrypted.setText(sharedPreferences().getString("DATA", ""))

        encode.setOnClickListener({ _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                hintTouchSensor()
                FingerPrintEncryption(this, AesKeyProperties(KEY_NAME), onAuthorizeFailure = this::playAnimBackwards)
                        .encode(textPlain.text.toString().toByteArray(Charsets.UTF_8))
                        {
                            playAnimForwards()
                            val encryptedBase16 = Base16().encode(it)
                            textEncrypted.setText(encryptedBase16)
                            sharedPreferences().edit().putString("DATA", encryptedBase16).apply()
                        }
            }

            //startFingerprint(fingerprintWrapper, textPlain.text.toString(), true)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                showAuthenticationScreen(123)
//                //textEncrypted.text = encode(textPlain.text.toString())
//            }
        })

        decode.setOnClickListener({ _ ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                hintTouchSensor()
                FingerPrintEncryption(this, AesKeyProperties(KEY_NAME), onAuthorizeFailure = this::playAnimBackwards)
                        .decode(Base16().decode(textEncrypted.text.toString()))
                        {
                            playAnimForwards()
                            textPlain.setText(String(it, Charsets.UTF_8))
                        }
            }

            // startFingerprint(fingerprintWrapper, textEncrypted.text.toString(), false)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                showAuthenticationScreen(456)
//                //textPlain.text = decode(textEncrypted.text.toString())
//            }
        })
    }

    private fun playAnimBackwards() {
        lottieAnimationView.setSpeed(-1f)
        lottieAnimationView.playAnimation()
    }

    val KEY_NAME = "my_key_9"

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showAuthenticationScreen(requestCode: Int) {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null)
        if (intent != null) {
            startActivityForResult(intent, requestCode)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun encode(message: String): String {
        val encoderDecoder = encoderDecoder()
        val encode = encoderDecoder.encode(message)
        sharedPreferences().edit().putString("DATA", encode).apply()
        return encode
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 123) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    startFingerprint(fingerprintWrapper, textPlain.text.toString(), true)
                //textEncrypted.text = encode(textPlain.text.toString())
            } else if (requestCode == 456) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    startFingerprint(fingerprintWrapper, textPlain.text.toString(), false)
                //textPlain.text = decode(textEncrypted.text.toString())
            }
        } else {
            Toast.makeText(this, "Confirming credentials failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharedPreferences() = getSharedPreferences("DATA_STORE", Context.MODE_PRIVATE)

    @RequiresApi(Build.VERSION_CODES.M)
    private fun decode(message: String): String {
        val encoderDecoder: MyAsymmetricEncoder = encoderDecoder()
        return encoderDecoder.decode(message)
    }

    private fun startFingerprint(fingerprintWrapper: FingerprintWrapper, message: String, encode: Boolean) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        if (fingerprintWrapper.canTakeFingerprint) {
            //val message = "68c9da8344ed109559b710f769e90ccc|a0c2233b68a348118a69d6419c8077d61f29ac8c3a28f1767cb1a474b293b621"

            val myAsymmetricEncoder = encoderDecoder()
            //val cipher = myAsymmetricEncoder.encryptCipher
            val cipher = if (encode) myAsymmetricEncoder.encryptCipher else {
                val byteArray = message
                        .split("|")
                        .map(Base16()::decode)

                val iv = byteArray[0]
                myAsymmetricEncoder.decyrptCipher(iv)
            }

            val cryptoObject = FingerprintManagerCompat.CryptoObject(cipher)

            //signature.initSign(key)

            hintTouchSensor()

            fingerprintWrapper.manager.authenticate(cryptoObject, 0, null,
                    object : FingerprintManagerCompat.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            playAnimForwards()

                            if (encode) {
                                val encrypted = myAsymmetricEncoder.encode(message)
                                textEncrypted.setText(encrypted)
                                sharedPreferences().edit().putString("DATA", encrypted).apply()
                            } else {
                                val byteArray = message
                                        .split("|")
                                        .map(Base16()::decode)
                                val data = byteArray[1]
                                val originalBytes = cipher.doFinal(data)
                                val original = String(originalBytes, Charsets.UTF_8)
                                textPlain.setText(original)
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            playAnimBackwards()
                            //Toast.makeText(this@FingerPrintDemoActivity, "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }, null)


        }
    }

    private fun playAnimForwards() {
        lottieAnimationView.setSpeed(1f)
        lottieAnimationView.playAnimation()
    }

    private fun hintTouchSensor() {
        Toast.makeText(this@FingerPrintDemoActivity, "Touch sensor", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun encoderDecoder() = MyAsymmetricEncoder(findOrCreateKeyPair(KEY_NAME))

    @RequiresApi(Build.VERSION_CODES.M)
    private fun findOrCreateKeyPair(KEY_NAME: String): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val key = keyStore.getKey(KEY_NAME, null) as? SecretKey

        if (key == null) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(
                        KeyGenParameterSpec.Builder(KEY_NAME,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                // Require the user to authenticate with a fingerprint to authorize every use
                                // of the key
                                .setUserAuthenticationRequired(true)

                                //If we use this, then you can use the system auth, otherwise you must use your own
                                //.setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .build())
                Log.d("ALAN", "New key")
                return generateKey()
            }
        } else {
            Log.d("ALAN", "Using existing key")
            //Toast.makeText(this@FingerPrintDemoActivity, "Key found", Toast.LENGTH_SHORT).show()
        }
        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }

}

class FingerprintWrapper(context: Context) {

    val manager = FingerprintManagerCompat.from(context)

    val canTakeFingerprint: Boolean = manager.isHardwareDetected && manager.hasEnrolledFingerprints()
}

private class MyAsymmetricEncoder(private val key: SecretKey) {
    private val base16 = io.github.novacrypto.security.Base16()

    val encryptCipher: Cipher = uninitializedCipher().apply {
        init(Cipher.ENCRYPT_MODE, key)
    }

    fun decyrptCipher(iv: ByteArray): Cipher = uninitializedCipher().apply {
        init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
    }

    private fun uninitializedCipher() =
            Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7)

    fun encode(byteArray: ByteArray): String {
        return base16.encode(encryptCipher.doFinal(byteArray))
    }

    fun encode(message: String): String {
        return base16.encode(encryptCipher.iv) + '|' + encode(message.toByteArray(Charsets.UTF_8))
    }

    fun decode(message: String): String {
        val byteArray = message
                .split("|")
                .map(Base16()::decode)
        val iv = byteArray[0]
        val decryptCipher = decyrptCipher(iv)
        val data = byteArray[1]
        val originalBytes = decryptCipher.doFinal(data)
        return String(originalBytes, Charsets.UTF_8)
    }
}