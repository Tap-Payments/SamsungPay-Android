package com.tap.company.samsungpay_sdk

//import company.tap.tapuilibrary.themekit.ThemeManager
//import company.tap.tapuilibrary.uikit.atoms.*
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import company.tap.tapbenefitpay.getQueryParameterFromUri
import company.tap.tapbenefitpay.open.web_wrapper.CardWebUrlPrefix
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
import org.json.JSONObject
import java.io.IOException


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
    private var samsungCheckoutStarted = false
    private var tapUrlLoaded = false
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
           // cacheMode = WebSettings.LOAD_NO_CACHE
           // mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW


        }
        //cardWebview.setBackgroundColor(Color.TRANSPARENT)
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

        // Extract public key from configuration
        val publicKey = getPublicKeyFromConfiguration(configuraton)

        // Determine if test or production environment based on public key
        val isTestMode = publicKey?.startsWith("pk_test_") ?: false

        callConfigAPI(configuraton, isTestMode)
        //    applyTheme()

    }

    /**
     * Extracts the public key from the configuration HashMap
     * Searches through operator section to find the publicKey
     */
    private fun getPublicKeyFromConfiguration(configuraton: java.util.HashMap<String, Any>): String? {
        return try {
            val operator = configuraton["operator"] as? Map<String, Any>
            operator?.get("publicKey") as? String
        } catch (e: Exception) {
            Log.e("PublicKeyExtraction", "Error extracting public key: ${e.message}")
            null
        }
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

            val url = request?.url?.toString() ?: return false
            Log.e("webview-url", url)
            /**
             * 3️⃣ Samsung Wallet / App Store deep link
             */
            if (url.startsWith(SAMSUNG_PAY_URL_PREFIX, true) ||
                url.startsWith(SAMSUNG_APP_STORE_URL, true)) {

                // Stop the WebView from continuing to load this URL
                webView?.post {
                    webView.stopLoading()
                    webView?.visibility = View.GONE

                }

                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val installIntent = Intent.parseUri(
                        "samsungapps://ProductDetail/com.samsung.android.spay",
                        Intent.URI_INTENT_SCHEME
                    )
                    installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(installIntent)
                }

                return true // ensures WebView does not handle the URL further
            }


            /**
             * 1️⃣ Handle Samsung Pay SDK callbacks
             */
          //  if(!tapUrlLoaded) {

                if (url.startsWith(CardWebUrlPrefix, ignoreCase = true)) {

                    when {
                        url.contains(SamsungPayStatusDelegate.onReady.name) -> {
                            SamsungPayDataConfiguration.getTapCardStatusListener()
                                ?.onSamsungPayReady()
                        }

                        url.contains(SamsungPayStatusDelegate.onChargeCreated.name) -> {
                            SamsungPayDataConfiguration.getTapCardStatusListener()
                                ?.onSamsungPayChargeCreated(
                                    request?.url?.getQueryParameterFromUri(keyValueName).toString()
                                )
                        }

                        url.contains(SamsungPayStatusDelegate.onOrderCreated.name) -> {
                            SamsungPayDataConfiguration.getTapCardStatusListener()
                                ?.onSamsungPayOrderCreated(
                                    request?.url?.getQueryParameter(keyValueName).toString()
                                )
                        }

                        url.contains(SamsungPayStatusDelegate.onClick.name) -> {
                            pair = Pair("", false)
                            onSuccessCalled = false
                            SamsungPayDataConfiguration.getTapCardStatusListener()
                                ?.onSamsungPayClick()
                            tapUrlLoaded = true
                            return true
                        }

                        url.contains(SamsungPayStatusDelegate.onCancel.name) -> {
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (!onSuccessCalled) {
                                    SamsungPayDataConfiguration.getTapCardStatusListener()
                                        ?.onSamsungPayCancel()
                                }
                            }, 3000)
                            webView?.post {
                                webView.stopLoading()
                                webView?.visibility = View.GONE
                            }
                            if (!(pair.first.isNotEmpty() && pair.second)) {
                                dismissDialog()
                            }

                        }

                        url.contains(SamsungPayStatusDelegate.onError.name) -> {

                            Handler(Looper.getMainLooper()).postDelayed({
                                if (!onSuccessCalled) {
                                    SamsungPayDataConfiguration.getTapCardStatusListener()
                                        ?.onSamsungPayError(
                                            request?.url?.getQueryParameterFromUri(keyValueName)
                                                .toString()
                                        )
                                }
                            }, 3000)

                            pair = Pair(
                                request?.url?.getQueryParameterFromUri(keyValueName).toString(),
                                true
                            )

                            closePayment()
                        }

                        url.contains(SamsungPayStatusDelegate.onSuccess.name) -> {

                            onSuccessCalled = true

                            pair = Pair(
                                request?.url?.getQueryParameterFromUri(keyValueName).toString(),
                                true
                            )

                            if (iSAppInForeground) {
                                closePayment()
                                Log.e("success", "one")
                            }
                        }
                    }

                    return true
                }
          //  }






            return false
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

        }


   /*     override fun shouldInterceptRequest(
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

                        *//**
                         * onBackPressed in Dialog
                         *//*
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

                else -> {

                }
            }

            return super.shouldInterceptRequest(view, request)
        }*/


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
        if (!onSuccessCalled) {
            // Sheet was likely canceled
            samsungCheckoutStarted = false
            SamsungPayDataConfiguration.getTapCardStatusListener()?.onSamsungPayCancel()
            Log.e("SamsungPay", "Sheet was closed/canceled")
            init(cardConfiguraton)
            cardWebview?.post {
                cardWebview?.visibility = View.VISIBLE

            }
          /*  val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(launchIntent)*/
        }

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


    @SuppressLint("RestrictedApi")
    private fun callConfigAPI(configuraton: java.util.HashMap<String, Any>, isTestMode: Boolean = true) {
       val baseURL = "https://mw-sdk.beta.tap.company/v2/button/config"
       // val baseURL = "https://mw-sdk.tap.company/v2/button/config"

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()



        // ✅ Convert HashMap → JSONObject dynamically
        val jsonObject = JSONObject(configuraton as Map<*, *>)

        // ✅ Select encryption key based on test/prod mode
        val encryptionKey = if (isTestMode) {
            context.resources.getString(R.string.enryptkeyTest)
        } else {
            context.resources.getString(R.string.enryptkeyProduction)
        }

        // ✅ Inject ONLY headers section with conditional encryption
        val headersObject = JSONObject().apply {
            put("application", NetworkApp.getApplicationInfo())
            put(
                "mdn",
                CryptoUtil.encryptJsonString(
                    "tap.BenefitPayExampleApp",
                    encryptionKey
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
            .addHeader("content-type", "application/json")
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




