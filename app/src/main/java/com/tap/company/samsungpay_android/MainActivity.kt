package com.tap.company.samsungpay_android

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tap.company.samsungpay_sdk.SamsungPayConfiguration
import com.tap.company.samsungpay_sdk.TapSamsungPayStatusDelegate
import java.util.Formatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() , TapSamsungPayStatusDelegate {
    //  var publicKeyLive:String = "pk_live_0zHLeUTOXBNEyJ8p6csbK52m"
    var publicKeyLive: String = "pk_live_3zIsCFeStGLv8DNd9m054bYc"
    var amount: Double = 1.0
    var currency: String = "BHD"
    var transactionReference: String = ""
    var postUrl: String = ""
    var secretString = "sk_live_x28QGHEwiVet6yKq07zMOrjU"
    val number3digits: String = String.format("%.3f", amount)
    lateinit var dataTextView: TextView

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configureSdk()
        val stringmsg =
            "x_publickey${publicKeyLive}x_amount${number3digits}x_currency${currency}x_transaction${transactionReference}x_post$postUrl"
        Log.e("stringMessage", stringmsg.toString())
        val string = Hmac.digest(msg = stringmsg, key = secretString)
        Log.e("encrypted hashString", string.toString())

        dataTextView = findViewById(R.id.data)
    }


    fun configureSdk() {


        /**
         * operator
         */
        val publicKey = "pk_live_3zIsCFeStGLv8DNd9m054bYc"
        //  val publicKey = intent.getStringExtra("publicKey")
        val hashStringKey = intent.getStringExtra("hashStringKey")
        val scopeKey = intent.getStringExtra("scopeKey")
        val operator = HashMap<String, Any>()
        operator.put("publicKey", publicKey.toString())
        operator.put("hashString", hashStringKey.toString())
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
        order.put("amount", if (orderAmount?.isEmpty() == true) "1" else orderAmount.toString())
        order.put("currency", selectedCurrency)
        order.put("description", orderDescription ?: "")
        order.put("reference", orderRefrence ?: "")
        order.put("metadata", metada)
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
        phone.put("countryCode", intent.getStringExtra("editPhoneCodeKey") ?: "+965")
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

        val paymentMethod = intent.getStringExtra("paymentMethodKey")


        Log.e(
            "interfaceData",
            "language" + selectedLanguage.toString() + "cardedge " + selectedCardEdge.toString()
        )
        val interfacee = HashMap<String, Any>()
        interfacee.put("locale", selectedLanguage ?: "en")
        //  interfacee.put("theme",selectedTheme ?: "light")
        interfacee.put("edges", selectedCardEdge ?: "circular")


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
        transaction.put(
            "amount",
            if (orderAmount?.isEmpty() == true) "1" else orderAmount.toString()
        )
        transaction.put("currency", "USD")
        transaction.put(
            "autoDismiss",
            "false")
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
        customer.put("names", listOf(name))




        configuration.put("paymentMethod", paymentMethod ?: "samsungpay")
        configuration.put("merchant", merchant)
        configuration.put("scope", scopeKey.toString())
        configuration.put("redirect", "tapredirectionwebsdk://") // TODO what will be in this
        configuration.put("customer", customer)
        configuration.put("interface", interfacee)
        configuration.put("reference", reference)
        configuration.put("metadata", "")
        configuration.put("post", post)
        configuration.put("transaction", transaction)
        configuration.put("operator", operator)




        SamsungPayConfiguration.configureWithTapSamsungPayDictionaryConfiguration(
            this,
            findViewById(R.id.benfit_pay),
            configuration,
            this
        )


    }


    override fun onSamsungPaySuccess(data: String) {
        dataTextView.text = "onSamsungPaySuccess >>" + "\n" +
                data + "\n" + ">>>>>>>>>>>>>>>>>>>>>>>"
    }

    override fun onSamsungPayReady() {
        super.onSamsungPayReady()
        dataTextView.text = "<<< onSamsungPayReady >>>" + "\n"


    }

    override fun onSamsungPayClick() {
        super.onSamsungPayClick()
    }

    override fun onSamsungPayChargeCreated(data: String) {
        super.onSamsungPayChargeCreated(data)
        dataTextView.text = "<<<onSamsungPayChargeCreated>>>" + "\n" +
                data + "\n" + ">>>>>>>>>>>>>>>>>>>>>>>"
    }

    override fun onSamsungPayOrderCreated(data: String) {
        super.onSamsungPayOrderCreated(data)
    }

    override fun onSamsungPayCancel() {
        super.onSamsungPayCancel()

        dataTextView.text = "onSamsungPayCancel is >>" + "\n" + "Cancelled!!!"


    }

    override fun onSamsungPayError(error: String) {

        dataTextView.text = "Result is :: " + error
        Log.e("onError RECIEVED ::", error.toString())
    }


}