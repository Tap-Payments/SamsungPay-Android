package com.tap.company.samsungpay_sdk

import Headers
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.tap.company.samsungpay_sdk.SamsungPayDataConfiguration.configurationsAsHashMap

import company.tap.tapbenefitpay.open.web_wrapper.HeadersApplication
import company.tap.tapbenefitpay.open.web_wrapper.HeadersMdn
import company.tap.tapbenefitpay.open.web_wrapper.headersKey
import company.tap.tapbenefitpay.open.web_wrapper.operatorKey
import company.tap.tapbenefitpay.open.web_wrapper.publicKeyToGet
import company.tap.tapbenefitpay.open.web_wrapper.urlWebStarter
import company.tap.tapnetworkkit.connection.NetworkApp
import company.tap.tapnetworkkit.utils.CryptoUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class SamsungPayConfiguration {

    companion object {

        private val retrofit = ApiServiceBenefit.RetrofitClient.getClient()
        private val tapSDKConfigsUrl = retrofit.create(ApiServiceBenefit.TapSDKConfigUrls::class.java)
        private var testEncKey: String? = null
        private var prodEncKey: String? = null
        private var dynamicBaseUrlResponse: String? = null
       var configApiUrl : String = "https://mw-sdk.tap.company/v2/button/config"
        fun configureWithTapSamsungPayDictionaryConfiguration(
            context: Context,
            tapCardInputViewWeb: TapSamsungPay?,
            tapMapConfiguration: java.util.HashMap<String, Any>,
            tapSamsungPayStatusDelegate: TapSamsungPayStatusDelegate? = null
        ) {
     /*       with(tapMapConfiguration) {
                Log.e("map", tapMapConfiguration.toString())
                configurationsAsHashMap = tapMapConfiguration
                val operator = configurationsAsHashMap?.get(operatorKey) as HashMap<*, *>
                val publickKey = operator.get(publicKeyToGet)

                val appLifecycleObserver = AppLifecycleObserver()
                ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

                addOperatorHeaderField(
                    tapCardInputViewWeb,
                    context,
                    CardConfiguraton.MapConfigruation,
                    publickKey.toString()
                )

                DataConfiguration.addTapSamsungPayStatusDelegate(tapBenefitPayStatusDelegate)
                tapCardInputViewWeb?.init(CardConfiguraton.MapConfigruation)

            }*/

            MainScope().launch {
                getTapSDKConfigUrls(
                    tapMapConfiguration,
                    tapCardInputViewWeb,
                    context,
                    tapSamsungPayStatusDelegate
                )
            }

        }

        private suspend fun getTapSDKConfigUrls(
            tapMapConfiguration: HashMap<String, Any>,
            tapCardInputViewWeb: TapSamsungPay?,
            context: Context,
            tapSamsungPayStatusDelegate : TapSamsungPayStatusDelegate?
        ) {

            try {
                /**
                 * request to get Tap configs
                 */

                val tapSDKConfigUrlResponse = tapSDKConfigsUrl.getSDKConfigUrl()
               // BASE_URL = tapSDKConfigUrlResponse.baseURL
                configApiUrl = tapSDKConfigUrlResponse.baseURL
               // configApiUrl = "https://mw-sdk.beta.tap.company/v2/button/config"
                testEncKey = tapSDKConfigUrlResponse.testEncKey
                prodEncKey = tapSDKConfigUrlResponse.prodEncKey
                urlWebStarter = tapSDKConfigUrlResponse.baseURL

                startSDKWithConfigs(
                    tapMapConfiguration,
                    tapCardInputViewWeb,
                    context,
                    tapSamsungPayStatusDelegate
                )

            } catch (e: Exception) {
             //   BASE_URL = urlWebStarter
                testEncKey =  tapCardInputViewWeb?.context?.resources?.getString(R.string.enryptkeyTest)
                prodEncKey = tapCardInputViewWeb?.context?.resources?.getString(R.string.enryptkeyProduction)

                startSDKWithConfigs(
                    tapMapConfiguration,
                    tapCardInputViewWeb,
                    context,
                    tapSamsungPayStatusDelegate,

                )
                Log.e("error Config", e.message.toString())
            }
        }

        @SuppressLint("SuspiciousIndentation", "RestrictedApi")
        fun addOperatorHeaderField(
            tapCardInputViewWeb: TapSamsungPay?,
            context: Context,
            modelConfiguration: CardConfiguraton,
            publicKey: String?
        ) {
         val encodedeky = getPublicEncryptionKey(publicKey,tapCardInputViewWeb)

            Log.e("packagedname",context.packageName.toString())

            NetworkApp.initNetwork(
                tapCardInputViewWeb?.context ,
                publicKey ?: "",
               context.packageName,
                ApiServiceBenefit. BASE_URL.replace("benefitpay?configurations", ""),
                "android-benefitpay",
                true,
                encodedeky,
                null
            )
            val headers = Headers(
                application = NetworkApp.getApplicationInfo(),
                mdn = CryptoUtil.encryptJsonString(
                    context.packageName.toString(),
                    encodedeky,
                )
            )

            when (modelConfiguration) {
                CardConfiguraton.MapConfigruation -> {
                    val hashMapHeader = HashMap<String, Any>()
                    hashMapHeader[HeadersMdn] = headers.mdn.toString()
                    hashMapHeader[HeadersApplication] = headers.application.toString()
                    configurationsAsHashMap?.put(headersKey, hashMapHeader)

                }
                else -> {}
            }


        }
        private fun getPublicEncryptionKey(
            publicKey: String?,
            tapCardInputViewWeb: TapSamsungPay?
        ): String? {
            if (!testEncKey.isNullOrBlank() && !prodEncKey.isNullOrBlank()) {
                return if (publicKey?.contains("test") == true) {
                    // println("EncKey>>>>>" + testEncKey)
                    testEncKey
                } else {
                    //  println("EncKey<<<<<<" + prodEncKey)
                    prodEncKey
                }
            } else {
                //  println("EncKey<<<<<<>>>>>>>>>" + testEncKey)
                return if (publicKey?.contains("test") == true) {
                    tapCardInputViewWeb?.context?.resources?.getString(R.string.enryptkeyTest)
                }else{
                    tapCardInputViewWeb?.context?.resources?.getString(R.string.enryptkeyProduction)
                }


            }
        }

        private fun startSDKWithConfigs(
            tapMapConfiguration: java.util.HashMap<String, Any>,
            tapCardInputViewWeb: TapSamsungPay?,
            context: Context,
            tapSamsungPayStatusDelegate: TapSamsungPayStatusDelegate? = null,

            ) {
            with(tapMapConfiguration) {
                Log.e("map", tapMapConfiguration.toString())
                configurationsAsHashMap = tapMapConfiguration
                val operator = configurationsAsHashMap?.get(operatorKey) as HashMap<*, *>
                val publickKey = operator.get(publicKeyToGet)

                val appLifecycleObserver = AppLifecycleObserver()
                ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

                addOperatorHeaderField(
                    tapCardInputViewWeb,
                    context,
                    CardConfiguraton.MapConfigruation,
                    publickKey.toString()
                )
                tapMapConfiguration.put("platform","mobile")
                SamsungPayDataConfiguration.addTapSamsungPayStatusDelegate(tapSamsungPayStatusDelegate)
                tapCardInputViewWeb?.init(tapMapConfiguration)

            }
        }
    }

}


