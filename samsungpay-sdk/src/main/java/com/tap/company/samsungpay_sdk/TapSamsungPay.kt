package com.tap.company.samsungpay_sdk


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import com.tap.company.samsungpay_sdk.SamsungPayConfiguration.Companion.interceptedStatus
import com.tap.company.samsungpay_sdk.SamsungPayConfiguration.Companion.interceptedURL
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
    private var isSamsungResponseRequest = false

    // True once the WebView delivers ANY authoritative result (success/charge/order/error).
    // Used to distinguish a real cancel from a payment whose result simply hasn't
    // arrived through the WebView yet when the user returns to the app.
    private var paymentResultReceived = false
    private val cancelHandler = Handler(Looper.getMainLooper())
    private var pendingCancelRunnable: Runnable? = null
    companion object {
        lateinit var cardWebview: WebView
        // lateinit var cardConfiguraton: CardConfiguraton
        private const val SAMSUNG_PAY_URL_PREFIX: String = "samsungpay"
        private const val SAMSUNG_APP_STORE_URL: String = "samsungapps://ProductDetail/com.samsung.android.spay"
        // How long to wait after returning from Samsung Wallet for the WebView to deliver a
        // result before treating the return as a user cancel.
        private const val CANCEL_GRACE_PERIOD_MS: Long = 3000

        /**
         * Injected at document-start. Wraps fetch + XMLHttpRequest so every API call made by
         * the page is reported back to native with its request body, response body, status,
         * method and url. This is the only reliable way to read POST bodies from a WebView.
         */
        private const val NETWORK_CAPTURE_JS: String = """
            (function() {
              if (window.__tapNetPatched) return;
              window.__tapNetPatched = true;
              function report(d) {
                try { AndroidNetworkLogger.onNetworkCall(JSON.stringify(d)); } catch (e) {}
              }
              // ----- fetch -----
              var origFetch = window.fetch;
              if (origFetch) {
                window.fetch = function(input, init) {
                  var url = (typeof input === 'string') ? input : (input && input.url);
                  var method = (init && init.method) || (input && input.method) || 'GET';
                  var reqBody = (init && init.body) ? String(init.body) : null;
                  return origFetch.apply(this, arguments).then(function(resp) {
                    try {
                      resp.clone().text().then(function(text) {
                        report({ source:'fetch', url:url, method:method, requestBody:reqBody, status:resp.status, responseBody:text });
                      });
                    } catch (e) {
                      report({ source:'fetch', url:url, method:method, requestBody:reqBody, status:resp.status });
                    }
                    return resp;
                  });
                };
              }
              // ----- XMLHttpRequest -----
              var origOpen = XMLHttpRequest.prototype.open;
              var origSend = XMLHttpRequest.prototype.send;
              XMLHttpRequest.prototype.open = function(method, url) {
                this.__tap = { method: method, url: url };
                return origOpen.apply(this, arguments);
              };
              XMLHttpRequest.prototype.send = function(body) {
                var self = this;
                var info = this.__tap || {};
                info.requestBody = body ? String(body) : null;
                this.addEventListener('load', function() {
                  var respText = '';
                  try { respText = self.responseText; } catch (e) {}
                  report({ source:'xhr', url:info.url, method:info.method, requestBody:info.requestBody, status:self.status, responseBody:respText });
                });
                return origSend.apply(this, arguments);
              };
            })();
        """
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
        val isDebuggable =
            (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // Enable chrome://inspect for this WebView, but only when the host app is
        // debuggable so it never ships enabled in a release build.
        if (isDebuggable) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        // progressBar = findViewById(R.id.progress_circular)
        with(cardWebview.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            // cacheMode = WebSettings.LOAD_NO_CACHE
            // mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        }
        cardWebview.setBackgroundColor(Color.WHITE)
        cardWebview.setLayerType(LAYER_TYPE_SOFTWARE, null)
        cardWebview.webViewClient = MyWebViewClient()
        // cardWebview.webChromeClient = WebChromeClient()

        // --- Network capture (request + response bodies) ---
        // WebResourceRequest in shouldInterceptRequest does NOT expose the POST body,
        // so we monkey-patch fetch/XHR in JS and bridge the data back through this interface.
        // SECURITY: payment bodies can contain card data — only capture/log in debug builds.
        if (isDebuggable) {
            cardWebview.addJavascriptInterface(NetworkCaptureBridge(), "AndroidNetworkLogger")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                // Injected before the page's own scripts run, so no early calls are missed.
                WebViewCompat.addDocumentStartJavaScript(cardWebview, NETWORK_CAPTURE_JS, setOf("*"))
            }
        }

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
                    samsungCheckoutStarted= true
                    paymentResultReceived = false
                    onSuccessCalled = false
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
                        markPaymentResultReceived()
                        SamsungPayDataConfiguration.getTapCardStatusListener()
                            ?.onSamsungPayChargeCreated(
                                request?.url?.getQueryParameterFromUri(keyValueName).toString()
                            )
                    }

                    url.contains(SamsungPayStatusDelegate.onOrderCreated.name) -> {
                        markPaymentResultReceived()
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
                        cardWebview.post {
                            cardWebview?.visibility = View.GONE
                        }
                        return true
                    }

                    url.contains(SamsungPayStatusDelegate.onCancel.name) -> {
                        markPaymentResultReceived()
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
                        markPaymentResultReceived()
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
                        markPaymentResultReceived()
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


        /**
         * Intercepts every resource the WebView requests. NOTE: WebResourceRequest does
         * NOT expose the POST body — that's why request/response bodies are captured via
         * NETWORK_CAPTURE_JS instead. This override only sees url, method and headers.
         * Returning null lets the WebView load the request normally.
         */
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {

            request?.let {

                val requestUrl = it.url.toString()

                Log.e(
                    "WebViewIntercept",
                    "[${it.method}] $requestUrl  headers=${it.requestHeaders}"
                )

                if (!requestUrl.isNullOrBlank() &&
                    interceptedURL?.let { target ->
                        requestUrl.contains(target, ignoreCase = true)
                    } == true
                ) {

                    Log.i(
                        "SamsungPay",
                        "Matched intercepted URL: $requestUrl"
                    )

                    // Optional: save flag for NetworkCaptureBridge
                    isSamsungResponseRequest = true
                }
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


    /**
     * Receives every fetch / XHR call captured by NETWORK_CAPTURE_JS, including the
     * request and response bodies. Runs on a background (JS bridge) thread — do not
     * touch the UI here without posting to the main thread.
     *
     * The JSON payload has: source ("fetch"|"xhr"), url, method, requestBody, status, responseBody.
     */
    inner class NetworkCaptureBridge {
        @JavascriptInterface
        fun onNetworkCall(json: String) {
            try {
                val obj = JSONObject(json)
                val url = obj.optString("url")
                val method = obj.optString("method")
                val status = obj.optInt("status", -1)
                val requestBody = obj.optString("requestBody")
                val responseBody = obj.optString("responseBody")

                Log.e("WebViewNetwork", "[$method] $url -> $status")

                Log.e("WebViewNetwork", "  requestBody: $requestBody")
                Log.e("WebViewNetwork", "  responseBody: $responseBody")

                println("interceptedURL is"+interceptedURL)
                if (isSamsungResponseRequest) {

                    // Handle Samsung Pay cancellation
                    if (!responseBody.isNullOrBlank()) {
                        try {
                            val responseJson = JSONObject(responseBody)
                            val paymentStatus = responseJson.optString("status")
                            println("interceptedStatus here"+interceptedStatus)
                            if (interceptedStatus.equals(paymentStatus, ignoreCase = true)) {

                                Log.i("SamsungPay", "Payment cancelled by user")
                                isSamsungResponseRequest = false
                                Handler(Looper.getMainLooper()).post {

                                    SamsungPayDataConfiguration
                                        .getTapCardStatusListener()
                                        ?.onSamsungPayCancel()

                                    samsungCheckoutStarted = false
                                    onSuccessCalled = false
                                    iSAppInForeground = true

                                    init(cardConfiguraton)

                                    cardWebview.visibility = View.VISIBLE
                                }

                                return
                            }
                        } catch (e: Exception) {
                            Log.e("SamsungPay", "Failed to parse responseBody: ${e.message}")
                        }
                    }
                }


                // Hook point: forward to your own listener/analytics if needed

            } catch (e: Exception) {
                Log.e("WebViewNetwork", "Failed to parse captured call: ${e.message}")
            }
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

    /**
     * Called by any authoritative WebView result (success / charge / order / error / cancel).
     * Records that we got a real result and cancels any pending "assume cancel" runnable
     * that onEnterForeground may have scheduled.
     */
    private fun markPaymentResultReceived() {
        paymentResultReceived = true
        pendingCancelRunnable?.let { cancelHandler.removeCallbacks(it) }
        pendingCancelRunnable = null
    }

    override fun onEnterForeground() {
        iSAppInForeground = true
        Log.e("applifeCycle", "onEnterForeground")

        // The user came back from the Samsung Wallet app. We do NOT yet know whether they
        // paid or cancelled — Samsung Wallet never tells us; only the WebView redirect does.
        // So instead of firing cancel immediately (which races with a real onSuccess that
        // hasn't arrived through the WebView yet), we wait a grace period. If a real result
        // (success/charge/order/error) arrives in the meantime, markPaymentResultReceived()
        // cancels this runnable. If the grace period elapses with no result, it was a cancel.
//        if (samsungCheckoutStarted && !onSuccessCalled && !paymentResultReceived) {
//
//            pendingCancelRunnable?.let { cancelHandler.removeCallbacks(it) }
//
//            val runnable = Runnable {
//                pendingCancelRunnable = null
//                if (!onSuccessCalled && !paymentResultReceived) {
//                    // No result arrived → genuine cancel.
//                    samsungCheckoutStarted = false
////                    SamsungPayDataConfiguration.getTapCardStatusListener()?.onSamsungPayCancel()
//                    Log.e("SamsungPay", "Sheet was closed/canceled (no result after grace period)")
//                    init(cardConfiguraton)
//                    cardWebview?.postDelayed({
//                        cardWebview?.visibility = View.VISIBLE
//                    }, 1800)
//                } else {
//                    Log.e("SamsungPay", "Foreground cancel suppressed — payment result was received")
//                }
//            }
//            pendingCancelRunnable = runnable
//            cancelHandler.postDelayed(runnable, CANCEL_GRACE_PERIOD_MS)
//        }
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
      // val baseURL = "https://mw-sdk.beta.tap.company/v2/button/config"
        val baseURL = "https://mw-sdk.tap.company/v2/button/config"

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
                    context.packageName.toString(),
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




