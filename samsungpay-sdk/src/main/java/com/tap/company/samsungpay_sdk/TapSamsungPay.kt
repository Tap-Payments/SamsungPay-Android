package com.tap.company.samsungpay_sdk

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.core.os.postDelayed
import company.tap.tapbenefitpay.*

import company.tap.tapbenefitpay.open.web_wrapper.CardWebUrlPrefix

import company.tap.tapbenefitpay.open.web_wrapper.samsungPayCheckoutUrl

import company.tap.tapbenefitpay.open.web_wrapper.keyValueName
import company.tap.tapnetworkkit.connection.NetworkApp
import company.tap.tapnetworkkit.utils.CryptoUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
//import company.tap.tapuilibrary.themekit.ThemeManager
//import company.tap.tapuilibrary.uikit.atoms.*
import java.net.URISyntaxException


@SuppressLint("ViewConstructor")
class TapSamsungPay : LinearLayout, ApplicationLifecycle {
    lateinit var webViewFrame: LinearLayout
    lateinit var progressBar: ProgressBar
    private var isSamsungPayUrlIntercepted = false
    lateinit var dialog: Dialog
    var pair = Pair("", false)
    lateinit var linearLayout: LinearLayout
    var iSAppInForeground = true
    var onSuccessCalled = false
    lateinit var urlToBeloaded: String
    lateinit var cardConfiguraton: java.util.HashMap<String, Any>

    companion object {
        lateinit var cardWebview: WebView
        // lateinit var cardConfiguraton: CardConfiguraton
        private const val SAMSUNG_PAY_URL_PREFIX: String = "samsungpay"
        private const val SAMSUNG_APP_STORE_URL: String = "samsungapps://ProductDetail/com.samsung.android.spay"
    }

    /**
     * Simple constructor to use when creating a TapPayCardSwitch from code.
     *  @param context The Context the view is running in, through which it can
     *  access the current theme, resources, etc.
     **/
    constructor(context: Context) : super(context)

    /**
     *  @param context The Context the view is running in, through which it can
     *  access the current theme, resources, etc.
     *  @param attrs The attributes of the XML Button tag being used to inflate the view.
     *
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)


    init {
        LayoutInflater.from(context).inflate(R.layout.activity_samsung_pay_layout_wrapper, this)
        initWebView()

    }


    private fun initWebView() {
        cardWebview = findViewById(R.id.webview)
        webViewFrame = findViewById(R.id.webViewFrame)
        progressBar = findViewById(R.id.progress_circular)
        with(cardWebview.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }


        }
        cardWebview.setBackgroundColor(Color.TRANSPARENT)
        //cardWebview.setLayerType(LAYER_TYPE_SOFTWARE, null)
        cardWebview.webViewClient = MyWebViewClient()
       // cardWebview.webChromeClient = WebChromeClient()

    }



    fun init(configuraton: java.util.HashMap<String, Any>) {
        cardConfiguraton = configuraton
        // progressBar.visibility = VISIBLE
        SamsungPayDataConfiguration.addAppLifeCycle(this)
        val transaction = configuraton["transaction"] as? MutableMap<String, Any?>
        val autoDismiss = when {
            transaction?.containsKey("autoDismiss") == true -> transaction.remove("autoDismiss") as? Boolean
            transaction?.containsKey("autoDissmess") == true -> transaction.remove("autoDissmess") as? Boolean
            else -> null
        } ?: false

       // configuraton["autoDissmess"] = autoDismiss


        callConfigAPI(configuraton)
        //    applyTheme()

    }


    /*  private fun applyTheme() {
        */
    /**
     * need to be refactored : mulitple copies of same code
     *//*
        when(cardConfiguraton){
            CardConfiguraton.MapConfigruation ->{
                val tapInterface = BenefitPayDataConfiguration.configurationsAsHashMap?.get("interface") as? Map<*, *>
              setTapThemeAndLanguage(
                    this.context,
                    TapLocal.valueOf(tapInterface?.get("locale")?.toString() ?: TapLocal.en.name),
                  TapTheme.valueOf(tapInterface?.get("theme")?.toString() ?: TapTheme.light.name))
            }
            else -> {}
        }


    }*/




    inner class MyWebViewClient : WebViewClient() {
        @SuppressLint("SuspiciousIndentation")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun shouldOverrideUrlLoading(
            webView: WebView?,
            request: WebResourceRequest?
        ): Boolean {


            /**
             * main checker if url start with "tapCardWebSDK://"
             */

            if (request?.url.toString().startsWith(CardWebUrlPrefix, ignoreCase = true)) {
                Log.e("url", request?.url.toString())
                /**
                 * listen for states of cardWebStatus of onReady , onValidInput .. etc
                 */
                if (request?.url.toString().contains(SamsungPayStatusDelegate.onReady.name)) {
                    SamsungPayDataConfiguration.getTapCardStatusListener()?.onSamsungPayReady()
                    //  progressBar.visibility = GONE
                }
                if (request?.url.toString()
                        .contains(SamsungPayStatusDelegate.onChargeCreated.name)
                ) {
                    SamsungPayDataConfiguration.getTapCardStatusListener()
                        ?.onSamsungPayChargeCreated(
                            request?.url?.getQueryParameterFromUri(keyValueName).toString()
                        )
                    //  progressBar.visibility = GONE
                }

               if (request?.url.toString()
                        .contains(SamsungPayStatusDelegate.onOrderCreated.name)
                ) {
                    SamsungPayDataConfiguration.getTapCardStatusListener()
                        ?.onSamsungPayOrderCreated(
                            request?.url?.getQueryParameter(keyValueName).toString()
                        )
                    //  progressBar.visibility = GONE
                }
                if (request?.url.toString().contains(SamsungPayStatusDelegate.onClick.name)) {
                    // progressBar.visibility = VISIBLE
                  //  isSamsungPayUrlIntercepted = false
                    pair = Pair("", false)
                    SamsungPayDataConfiguration.getTapCardStatusListener()?.onSamsungPayClick()
                    onSuccessCalled = false


                }


                if (request?.url.toString().contains(SamsungPayStatusDelegate.onCancel.name)) {
                    android.os.Handler(Looper.getMainLooper()).postDelayed(3000) {
                        if (!onSuccessCalled) {
                            SamsungPayDataConfiguration.getTapCardStatusListener()
                                ?.onSamsungPayCancel()
                        }


                    }

                    if (!(pair.first.isNotEmpty() and pair.second)) {
                        dismissDialog()
                    }
                    // progressBar.visibility = GONE

                }

                if (request?.url.toString().contains(SamsungPayStatusDelegate.onError.name)) {
                    android.os.Handler(Looper.getMainLooper()).postDelayed(3000) {
                        if (!onSuccessCalled) {
                            SamsungPayDataConfiguration.getTapCardStatusListener()
                                ?.onSamsungPayError(
                                    request?.url?.getQueryParameterFromUri(keyValueName).toString()
                                )

                        }

                    }
                    pair =
                        Pair(request?.url?.getQueryParameterFromUri(keyValueName).toString(), true)
                    closePayment()
                    //  progressBar.visibility = GONE

                }

                if (request?.url.toString().contains(SamsungPayStatusDelegate.onSuccess.name)) {
                    onSuccessCalled = true
                    pair =
                        Pair(request?.url?.getQueryParameterFromUri(keyValueName).toString(), true)
                    when (iSAppInForeground) {

                        true -> {
                            closePayment()
                            Log.e("success", "one")
                        }

                        false -> {}
                    }
                    //  progressBar.visibility = GONE

                }

                return true

            }
            // add below if statement to check if URL is Samsung Pay or Samsung App Store deep link
            if (request?.url?.toString()?.startsWith(SAMSUNG_PAY_URL_PREFIX, ignoreCase = true) == true ||
                request?.url?.toString()?.startsWith(SAMSUNG_APP_STORE_URL, ignoreCase = true) == true) {
                try {
                    val intent = Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    // Samsung Wallet app is not installed → open Samsung App Store
                    val installIntent = Intent.parseUri(
                        "samsungapps://ProductDetail/com.samsung.android.spay",
                        Intent.URI_INTENT_SCHEME
                    )
                    installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(installIntent)
                }
                // return true will cause that the URL will not be loaded in WebView
                return true
            }


            return false

        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

        }


        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            Log.e("intercepted", request?.url.toString())


            when (request?.url?.toString()?.contains(samsungPayCheckoutUrl)
                ?.and((!isSamsungPayUrlIntercepted))) {

                true -> {
                    view?.post {
                        (webViewFrame as ViewGroup).removeView(cardWebview)


                        dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
                        //Create LinearLayout Dynamically
                        linearLayout = LinearLayout(context)
                        //Setup Layout Attributes
                        val params = LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        linearLayout.layoutParams = params
                        linearLayout.orientation = VERTICAL

                        /**
                         * onBackPressed in Dialog
                         */
                        dialog.setOnKeyListener { view, keyCode, keyEvent ->
                            if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                                dismissDialog()
                                init(cardConfiguraton)
                                return@setOnKeyListener true
                            }
                            return@setOnKeyListener false
                        }


                        if (cardWebview.parent == null) {
                            linearLayout.addView(cardWebview)
                        }

                        dialog.setContentView(linearLayout)
                        dialog.show()
                    }

                    isSamsungPayUrlIntercepted = true
                }

                else -> {}
            }

            return super.shouldInterceptRequest(view, request)
        }


        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {


            Log.e("error code", error.errorCode.toString())
            Log.e("error description ", error.description.toString())

            Log.e("request header ", request.requestHeaders.toString())

            super.onReceivedError(view, request, error)
        }

    }


    private fun dismissDialog() {
        if (::dialog.isInitialized) {
            linearLayout.removeView(cardWebview)
            dialog.dismiss()
            if (cardWebview.parent == null) {
                (webViewFrame as ViewGroup).addView(cardWebview)
            }
        }
    }

    override fun onEnterForeground() {
        iSAppInForeground = true
        Log.e("applifeCycle", "onEnterForeground")
        //  closePayment()


    }

    private fun closePayment() {

        if (pair.second) {
            Log.e("app", "one")
            dismissDialog()
            //  init(cardConfiguraton) // was reloading url cz problem stopped
            SamsungPayDataConfiguration.getTapCardStatusListener()?.onSamsungPaySuccess(pair.first)

        }
    }

    override fun onEnterBackground() {
        iSAppInForeground = false
        Log.e("applifeCycle", "onEnterBackground")

    }

    /*private fun callConfigAPI(configuraton: java.util.HashMap<String, Any>) {
        try {
            val baseURL = configApiUrl
            // val baseURL = "https://mw-sdk.beta.tap.company/v2/button/config"
            val builder: OkHttpClient.Builder = OkHttpClient().newBuilder()
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            builder.addInterceptor(interceptor)

            val body = (configuraton as Map<*, *>?)?.let { JSONObject(it).toString().toRequestBody("application/json".toMediaTypeOrNull()) }
            val okHttpClient: OkHttpClient = builder.build()
            val request: Request = Request.Builder()
                .url(baseURL)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        var responseBody: JSONObject? =
                            response.body?.string()?.let { JSONObject(it) } // toString() is not the response body, it is a debug representation of the response body

                        if(!responseBody.toString().contains("errors")){
                            var redirectURL = responseBody?.getString("redirect_url")
                            if (redirectURL != null) {
                                // knetWebView.loadUrl(redirectURL)
                                urlToBeloaded = redirectURL
                                Handler(Looper.getMainLooper()).post {
                                    cardWebview.loadUrl(redirectURL)

                                }
                            }
                        }else{


                        }

                    } catch (ex: JSONException) {
                        throw RuntimeException(ex)
                    } catch (ex: IOException) {
                        throw RuntimeException(ex)
                    }

                }

                override fun onFailure(call: Call, e: IOException) {}
            })
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }*/
    @SuppressLint("RestrictedApi")
    private fun callConfigAPI(configuraton: java.util.HashMap<String, Any>) {
        val baseURL = "https://mw-sdk.beta.tap.company/v2/button/config"

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

// Original JSON object
      /*  val jsonObject = JSONObject().apply {
            put("paymentMethod", "samsungpay")
            put("merchant", JSONObject().apply { put("id", "") })
            put("scope", "charge")
            put("redirect", "tappaybuttonwebsdk://")
            put("customer", JSONObject().apply {
                put("name", JSONArray().apply {
                    put(JSONObject().apply {
                        put("middle", "Middle")
                        put("last", "Payments")
                        put("lang", "en")
                        put("first", "Tap")
                    })
                })
                put("contact", JSONObject().apply {
                    put("phone", JSONObject().apply {
                        put("number", "66178990")
                        put("countryCode", "965")
                    })
                    put("email", "email@email.com")
                })
                put("id", "")
            })
            put("interface", JSONObject().apply {
                put("edges", "curved")
                put("locale", "en")
            })
            put("reference", JSONObject().apply {
                put("transaction", "")
                put("order", "")
            })
            put("metadata", "")
            put("post", JSONObject().apply { put("url", "") })
            put("order", JSONObject().apply {
                put("id", "")
                put("amount", 20)
                put("currency", "USD")
            })
            put("operator", JSONObject().apply {
                put("hashString", "")
                put("publicKey", "pk_test_Vlk842B1EA7tDN5QbrfGjYzh")
            })
            put("platform", "mobile")
            put("debug", true)

            // Add your custom headers inside JSON body
            put("headers", JSONObject().apply {
                put("application", NetworkApp.getApplicationInfo())
                put(
                    "mdn", CryptoUtil.encryptJsonString(
                        "tap.BenefitPayExampleApp",
                       context.resources.getString(R.string.enryptkeyTest)
                    )
                )
            })
        }*/


        // ✅ Convert HashMap → JSONObject dynamically
        val jsonObject = JSONObject(configuraton as Map<*, *>)

        // ✅ Inject ONLY headers section
        val headersObject = JSONObject().apply {
            put("application", NetworkApp.getApplicationInfo())
            put(
                "mdn",
                CryptoUtil.encryptJsonString(
                    "tap.BenefitPayExampleApp",
                    context.resources.getString(R.string.enryptkeyTest)
                )
            )
        }

        // ✅ Add / override headers in JSON
        jsonObject.put("headers", headersObject)

        val body = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

        println("body in >>"+jsonObject)
        val request = Request.Builder()
            .url(baseURL)
            .post(body)
           // .addHeader("sec-ch-ua-platform", "\"macOS\"")
           // .addHeader("Referer", "https://demo.dev.tap.company/")
           /* .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
            )*/
          /*  .addHeader(
                "sec-ch-ua",
                "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\""
            )*/
            .addHeader("content-type", "application/json")
           // .addHeader("sec-ch-ua-mobile", "?0")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) throw IOException("Unexpected code $it")

                    val responseBody = it.body?.string()
                    println(responseBody)

                    try {
                        val jsonResponse = JSONObject(responseBody ?: "")
                        if (!jsonResponse.toString().contains("errors")) {
                            val redirectURL = jsonResponse.optString("redirect_url")
                            println("Redirect URL: $redirectURL")
                            Handler(Looper.getMainLooper()).post {
                                cardWebview.loadUrl(redirectURL)
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        })

    }

}

enum class CardConfiguraton() {
    MapConfigruation
}




