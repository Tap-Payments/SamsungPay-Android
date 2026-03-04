package com.tap.company.samsungpay_android

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.widget.ScrollView
import android.widget.LinearLayout
import android.graphics.Color
import com.tap.company.samsungpay_sdk.SamsungPayConfiguration
import com.tap.company.samsungpay_sdk.TapSamsungPayStatusDelegate
import java.util.Formatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class MainActivity : AppCompatActivity() , TapSamsungPayStatusDelegate {

    var publicKeyLive: String = "pk_live_3zIsCFeStGLv8DNd9m054bYc"
    var amount: Double = 0.1
    var currency: String = "KWD"
    var transactionReference: String = ""
    var postUrl: String = ""
    var secretString = "pk_live_3zIsCFeStGLv8DNd9m054bYc"
    val number3digits: String = String.format("%.3f", amount)
    lateinit var dataTextView: TextView
    lateinit var logTextView: TextView

    object Hmac {
        fun digest(
            msg: String,
            key: String,
            alg: String = "HmacSHA256"
        ): String {
            val signingKey = SecretKeySpec(key.toByteArray(), alg)
            val mac = Mac.getInstance(alg)
            mac.init(signingKey)

            val bytes = mac.doFinal(msg.toByteArray())
            return format(bytes)
        }

        private fun format(bytes: ByteArray): String {
            val formatter = Formatter()
            bytes.forEach { formatter.format("%02x", it) }
            return formatter.toString()
        }
    }

    /**
     * Show result dialog with custom styling
     * @param title Dialog title
     * @param message Dialog message/result data
     * @param statusColor Color for the title bar
     */
    private fun showResultDialog(
        title: String,
        message: String,
        statusColor: Int = Color.parseColor("#2196F3")
    ) {
        // Create a scrollable text view for long messages
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val messageTextView = TextView(this).apply {
            text = message
            textSize = 14f
            setPadding(24, 16, 24, 16)
            setTextColor(Color.BLACK)
            setTextIsSelectable(true)
        }

        scrollView.addView(messageTextView)

        val titleTextView = TextView(this).apply {
            text = title
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(24, 16, 24, 16)
            setBackgroundColor(statusColor)
        }

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(titleTextView)
            addView(scrollView)
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
            .apply {
                // Adjust dialog width for better display
                window?.setLayout(
                    (resources.displayMetrics.widthPixels * 0.9).toInt(),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configureSdk()
        val stringmsg =
            "x_publickey${publicKeyLive}x_amount${number3digits}x_currency${currency}x_transaction${transactionReference}x_post$postUrl"
        Log.e("stringMessage", stringmsg.toString())
        val string = Hmac.digest(msg = stringmsg, key = publicKeyLive)
        Log.e("encrypted hashString", string.toString())

        dataTextView = findViewById(R.id.data)

    }


    fun configureSdk() {


        /**
         * operator
         */
      //  val publicKey = "pk_live_3zIsCFeStGLv8DNd9m054bYc"
          val publicKey = intent.getStringExtra("publicKey")
        val hashStringKey = intent.getStringExtra("hashStringKey")
        val scopeKey = "charge"
        val operator = HashMap<String, Any>()
        operator.put("publicKey", publicKey.toString())
        //operator.put("hashString", hashStringKey.toString())
        operator.put("hashString", "")

        Log.e("orderData", "pbulc" + publicKey.toString() + " \nhash" + hashStringKey.toString())

        /**
         * metadata
         */
        val metada = HashMap<String, Any>()
        metada.put("id", "")

        /**
         * order
         */
        val ordrId = intent.getStringExtra("orderIdKey")
        val orderDescription = intent.getStringExtra("orderDescKey")
        val orderAmount = intent.getStringExtra("amountKey")
        val orderRefrence = intent.getStringExtra("orderTransactionRefrence")
        val selectedCurrency: String = intent.getStringExtra("selectedCurrencyKey").toString()

        /**
         * order
         */
        val order = HashMap<String, Any>()
        order.put("id", ordrId ?: "")
        order.put("amount",0.1)
        order.put("currency", "KWD")
       // order.put("description", orderDescription ?: "")
       // order.put("reference", orderRefrence ?: "")
        //order.put("metadata", metada)
        Log.e(
            "orderData",
            "id" + ordrId.toString() + "  \n dest" + orderDescription.toString() + " \n orderamount " + orderAmount.toString() + "  \n orderRef" + orderRefrence.toString() + "  \n currency " + selectedCurrency.toString()
        )


        /**
         * merchant
         */
        val merchant = HashMap<String, Any>()
        merchant.put("id", "")

        /**
         * invoice
         */
        val invoice = HashMap<String, Any>()
        invoice.put("id", "")


        /**
         * phone
         */
        val phone = java.util.HashMap<String, Any>()
        phone.put("countryCode", intent.getStringExtra("editPhoneCodeKey") ?: "965")
        phone.put("number", intent.getStringExtra("editPhoneNoKey") ?: "6617090")


        /**
         * contact
         */
        val contact = java.util.HashMap<String, Any>()
        contact.put("email", intent.getStringExtra("editEmailKey") ?: "email@emailc.com")
        contact.put("phone", phone)


        /**
         * interface
         */

        val selectedLanguage: String? = intent.getStringExtra("selectedlangKey")


        val selectedCardEdge = intent.getStringExtra("selectedcardedgeKey")


      //  val paymentMethod = intent.getStringExtra("paymentMethodKey")
        val paymentMethod ="samsungpay"


        Log.e(
            "interfaceData",
            "language" + selectedLanguage.toString() + "cardedge " + selectedCardEdge.toString()
        )
        val interfacee = HashMap<String, Any>()
        interfacee.put("locale", selectedLanguage ?: "en")
        //  interfacee.put("theme",selectedTheme ?: "light")
        interfacee.put("edges", selectedCardEdge ?: "curved")


        val post = HashMap<String, Any>()
        post.put("url", "")
        val configuration = LinkedHashMap<String, Any>()

        /**
         * Transaction
         * ***/
        val transaction = HashMap<String, Any>()

        /**
         * authenticate for Card buttons
         */
        val authenticate = HashMap<String, Any>()
        authenticate.put("id", "")
        authenticate.put("required", true)
        /**
         * source for Card buttons
         */
        val source = HashMap<String, Any>()
        source.put("id", "")
     /*   transaction.put(
            "amount",
            if (orderAmount?.isEmpty() == true) "1" else orderAmount.toString()
        )*/
        transaction.put(
            "amount",
            1
        )
        transaction.put("currency", "KWD")
       /* transaction.put(
            "autoDismiss",
            "false")*/
        ///can be true/ false


        Log.e(
            "transaction",
            " \n orderamount " + orderAmount.toString() + "  \n currency " + selectedCurrency.toString()
        )

        /**
         * Reference */
        val reference = HashMap<String, Any>()

        reference.put("transaction", orderRefrence ?: "")
        reference.put("order", orderDescription ?: "")

        /**
         * name
         */
        val name = java.util.HashMap<String, Any>()
        name.put("lang", selectedLanguage ?: "en")
        name.put("first", intent.getStringExtra("editFirstNameKey") ?: "TAP")
        name.put("middle", intent.getStringExtra("editMiddleNameKey") ?: "middle")
        name.put("last", intent.getStringExtra("editLastNameKey") ?: "PAYMENTS")
        /**
         * Customer data
         */

        val customer = HashMap<String, Any>()
        customer.put("id", "")
        customer.put("contact", contact)
        customer.put("name", listOf(name))




        configuration.put("paymentMethod", "samsungpay")
        configuration.put("merchant", merchant)
        configuration.put("scope", intent.getStringExtra("scopeKey") ?: "charge")
        configuration.put("redirect", "tappaybuttonwebsdk://") // TODO what will be in this
        configuration.put("customer", customer)
        configuration.put("interface", interfacee)
        configuration.put("reference", reference)
        configuration.put("metadata", "")
        configuration.put("post", post)
        configuration.put("order", order)
        configuration.put("operator", operator)
        configuration.put("platform", "mobile")
        configuration.put("debug", true)


        SamsungPayConfiguration.configureWithTapSamsungPayDictionaryConfiguration(
            this,
            findViewById(R.id.samsung_pay),
            configuration,
            this
        )


    }


    override fun onSamsungPaySuccess(data: String) {
        dataTextView.text = "onSamsungPaySuccess >>" + "\n" +
                data + "\n" + ">>>>>>>>>>>>>>>>>>>>>>>"
        showResultDialog(
            "✓ Payment Success",
            data,
            Color.parseColor("#4CAF50")
        )
    }

    override fun onSamsungPayReady() {
        super.onSamsungPayReady()
        dataTextView.text = "<<< onSamsungPayReady >>>" + "\n"
        showResultDialog(
            "✓ Samsung Pay Ready",
            "Samsung Pay has been initialized and is ready for transactions.",
            Color.parseColor("#2196F3")
        )
    }

    override fun onSamsungPayClick() {
        super.onSamsungPayClick()
        dataTextView.text = "<<< onSamsungPayClick >>>" + "\n"
        showResultDialog(
            "Samsung Pay Clicked",
            "User clicked on Samsung Pay button.\nInitiating payment process...",
            Color.parseColor("#FF9800")
        )
    }

    override fun onSamsungPayChargeCreated(data: String) {
        super.onSamsungPayChargeCreated(data)
        dataTextView.text = "<<<onSamsungPayChargeCreated>>>" + "\n" +
                data + "\n" + ">>>>>>>>>>>>>>>>>>>>>>>"
        showResultDialog(
            "✓ Charge Created",
            data,
            Color.parseColor("#9C27B0")
        )
    }

    override fun onSamsungPayOrderCreated(data: String) {
        super.onSamsungPayOrderCreated(data)
        dataTextView.text = "<<<onSamsungPayOrderCreated>>>" + "\n" +
                data + "\n" + ">>>>>>>>>>>>>>>>>>>>>>>"
        showResultDialog(
            "✓ Order Created",
            data,
            Color.parseColor("#00BCD4")
        )
    }

    override fun onSamsungPayCancel() {
        super.onSamsungPayCancel()
        dataTextView.text = "onSamsungPayCancel is >>" + "\n" + "Cancelled!!!"
        showResultDialog(
            "⚠ Payment Cancelled",
            "The Samsung Pay transaction was cancelled by the user.",
            Color.parseColor("#FF5722")
        )
    }

    override fun onSamsungPayError(error: String) {
        dataTextView.text = "Result is :: " + error
        Log.e("onError RECIEVED ::", error.toString())
        showResultDialog(
            "✗ Error Occurred",
            error,
            Color.parseColor("#F44336")
        )
    }


}