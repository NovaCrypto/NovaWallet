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

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_finger_print_demo.*
import kotlinx.android.synthetic.main.content_finger_print_demo.*
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class FingerPrintDemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finger_print_demo)
        setSupportActionBar(toolbar)

        val fingerprintWrapper = FingerprintWrapper(this)

        start_fingerprint.isEnabled = fingerprintWrapper.canTakeFingerprint

        start_fingerprint.setOnClickListener({ _ ->
            startFingerprint(fingerprintWrapper)
        })
    }

    private fun startFingerprint(fingerprintWrapper: FingerprintWrapper) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        if (fingerprintWrapper.canTakeFingerprint) {
            val KEY_NAME = "my_key_2"

            val privateKey = findOrCreatePrivateKey(KEY_NAME)

            val signature = Signature.getInstance("SHA256withECDSA")
            val cryptoObject = FingerprintManagerCompat.CryptoObject(signature)

            signature.initSign(privateKey)

            fingerprintWrapper.manager.authenticate(cryptoObject, 0, null,
                    object : FingerprintManagerCompat.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            lottieAnimationView.setSpeed(1f)
                            lottieAnimationView.playAnimation()
                            //Toast.makeText(this@FingerPrintDemoActivity, "Authenticated " + result, Toast.LENGTH_SHORT).show()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            lottieAnimationView.setSpeed(-1f)
                            lottieAnimationView.playAnimation()
                            //Toast.makeText(this@FingerPrintDemoActivity, "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }, null)

        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun findOrCreatePrivateKey(KEY_NAME: String): PrivateKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val key = keyStore.getKey(KEY_NAME, null) as? PrivateKey

        if (key == null) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(
                        KeyGenParameterSpec.Builder(KEY_NAME,
                                KeyProperties.PURPOSE_SIGN)
                                .setDigests(KeyProperties.DIGEST_SHA256)
                                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                                // Require the user to authenticate with a fingerprint to authorize
                                // every use of the private key
                                .setUserAuthenticationRequired(true)
                                .build())
                generateKeyPair()
            }
        } else {
            Toast.makeText(this@FingerPrintDemoActivity, "Key found", Toast.LENGTH_SHORT).show()
        }
        return key ?: keyStore.getKey(KEY_NAME, null) as PrivateKey
    }

}

class FingerprintWrapper(context: Context) {

    val manager = FingerprintManagerCompat.from(context)

    val canTakeFingerprint: Boolean = manager.isHardwareDetected && manager.hasEnrolledFingerprints()
}
