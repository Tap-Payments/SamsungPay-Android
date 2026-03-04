package com.tap.company.samsungpay_sdk

import Customer
import TapAuthentication
import TapCardConfigurations
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.Log

import kotlin.collections.HashMap

/**
 * Created by AhlaamK on 3/23/22.

Copyright (c) 2022    Tap Payments.
All rights reserved.
 **/
@SuppressLint("StaticFieldLeak")
object SamsungPayDataConfiguration {

    private var tapSamsungPayStatusDelegate: TapSamsungPayStatusDelegate? = null
    private var applicationLifecycle: ApplicationLifecycle? = null

    var configurations: TapCardConfigurations? = null
        get() = field
        set(value) {
            field = value
        }

    var customerExample: Customer? = null
        get() = field
        set(value) {
            field = value
        }

    var authenticationExample: TapAuthentication? = null
        get() = field
        set(value) {
            field = value
        }
    var configurationsAsJson: String? = null
        get() = field
        set(value) {
            field = value
        }

    var configurationsAsHashMap: HashMap<String,Any>? = null
        get() = field
        set(value) {
            field = value
        }

    var lanuage: String? = null
        get() = field
        set(value) {
            field = value
        }









    fun setTheme(
        context: Context?,
        resources: Resources?,
        urlString: String?,
        urlPathLocal: Int?,
        fileName: String?
    ) {
        if (resources != null && urlPathLocal != null) {
            if (fileName != null && fileName.contains("dark")) {
                if (urlPathLocal != null) {
                   // ThemeManager.loadTapTheme(resources, urlPathLocal, "darktheme")
                }
            } else {
                if (urlPathLocal != null) {
                   // ThemeManager.loadTapTheme(resources, urlPathLocal, "lighttheme")
                }
            }
        } else if (urlString != null) {
            if (context != null) {
                println("urlString>>>" + urlString)
             //   ThemeManager.loadTapTheme(context, urlString, "lighttheme")
            }
        }

    }

    fun setLocale(
        context: Context,
        languageString: String,
        urlString: String?,
        resources: Resources?,
        urlPathLocal: Int?
    ) {
       // LocalizationManager.setLocale(context, Locale(languageString))
        lanuage = languageString
        if (resources != null && urlPathLocal != null) {
          //  LocalizationManager.loadTapLocale(resources, R.raw.lang)
        } else if (urlString != null) {
            if (context != null) {
             //   LocalizationManager.loadTapLocale(context, urlString)
                Log.e("local", urlString.toString())

            }
        }

    }

    fun setCustomer(customer: Customer) {
        customerExample = customer
    }


    fun setTapAuthentication(tapAuthentication: TapAuthentication) {
        authenticationExample = tapAuthentication
    }

    fun addTapSamsungPayStatusDelegate(_tapCardStatuDelegate: TapSamsungPayStatusDelegate?) {
        tapSamsungPayStatusDelegate = _tapCardStatuDelegate

    }
    fun addAppLifeCycle(appLifeCycle: ApplicationLifecycle?) {
        applicationLifecycle = appLifeCycle
    }

    fun getAppLifeCycle(): ApplicationLifecycle? {
        return applicationLifecycle
    }
    fun getTapCardStatusListener(): TapSamsungPayStatusDelegate? {
        return tapSamsungPayStatusDelegate
    }

    fun initializeSDK(activity: Activity, configurations: HashMap<String,Any>, tapSamsungPay: TapSamsungPay, tapSamsungPayStatusDelegate: TapSamsungPayStatusDelegate){
        SamsungPayConfiguration.configureWithTapSamsungPayDictionaryConfiguration(activity,tapSamsungPay,configurations , tapSamsungPayStatusDelegate)
    }






}

/**
 * Interface for handling Samsung Pay payment status callbacks
 *
 * This interface provides callback methods that are invoked at various stages
 * of the Samsung Pay payment transaction lifecycle. Implement this interface
 * in your Activity or Fragment to respond to payment events.
 *
 * @see TapSamsungPay
 * @see SamsungPayConfiguration
 */
interface TapSamsungPayStatusDelegate {

    /**
     * Called when the Samsung Pay payment is successful
     *
     * This callback is triggered after the user has successfully completed
     * the payment transaction. The response data contains transaction details
     * including order information, charge details, and transaction reference.
     *
     * @param data JSON string containing the complete transaction response
     *             Example:
     *             ```json
     *             {
     *               "id": "charge_id_xxx",
     *               "object": "charge",
     *               "amount": 100,
     *               "currency": "KWD",
     *               "status": "CAPTURED",
     *               "order": { ... },
     *               "customer": { ... }
     *             }
     *             ```
     *
     * @see onSamsungPayError
     * @see onSamsungPayCancel
     */
    fun onSamsungPaySuccess(data: String)

    /**
     * Called when the Samsung Pay SDK is ready to accept payment requests
     *
     * This callback is invoked after the SDK has been successfully initialized
     * and configured. The payment button is now active and ready for user interaction.
     * This is a good place to update your UI to indicate payment is ready.
     *
     * **Optional callback** - default implementation does nothing
     *
     * Example usage:
     * ```kotlin
     * override fun onSamsungPayReady() {
     *     super.onSamsungPayReady()
     *     paymentButton.isEnabled = true
     *     loadingIndicator.visibility = View.GONE
     * }
     * ```
     */
    fun onSamsungPayReady(){}

    /**
     * Called when the user clicks on the Samsung Pay button
     *
     * This callback is triggered when the user initiates the payment process
     * by clicking the Samsung Pay payment button. This is useful for logging,
     * analytics, or triggering intermediate UI changes.
     *
     * **Optional callback** - default implementation does nothing
     *
     * Example usage:
     * ```kotlin
     * override fun onSamsungPayClick() {
     *     super.onSamsungPayClick()
     *     Analytics.logEvent("payment_initiated")
     * }
     * ```
     */
    fun onSamsungPayClick(){}

    /**
     * Called when the order has been successfully created on the server
     *
     * This callback is triggered after the order entity has been created
     * on the Tap Payments server. The response contains the order details
     * including order ID, amount, currency, and status.
     *
     * **Optional callback** - default implementation does nothing
     *
     * @param data JSON string containing the order creation response
     *             Example:
     *             ```json
     *             {
     *               "id": "order_id_xxx",
     *               "object": "order",
     *               "amount": 100,
     *               "currency": "KWD",
     *               "status": "INITIATED",
     *               "reference": { ... },
     *               "items": [ ... ]
     *             }
     *             ```
     *
     * @see onSamsungPayChargeCreated
     */
    fun onSamsungPayOrderCreated(data: String){}

    /**
     * Called when a charge has been successfully created
     *
     * This callback is triggered after a charge entity has been created
     * on the Tap Payments server. This typically happens after order creation
     * and before the final payment authorization. The response contains charge
     * details including charge ID, amount, status, and authentication information.
     *
     * **Optional callback** - default implementation does nothing
     *
     * @param data JSON string containing the charge creation response
     *             Example:
     *             ```json
     *             {
     *               "id": "charge_id_xxx",
     *               "object": "charge",
     *               "amount": 100,
     *               "currency": "KWD",
     *               "status": "PENDING",
     *               "order": {
     *                 "id": "order_id_xxx"
     *               },
     *               "authentication": { ... }
     *             }
     *             ```
     *
     * @see onSamsungPaySuccess
     */
    fun onSamsungPayChargeCreated(data:String){}

    /**
     * Called when a payment error occurs
     *
     * This callback is invoked when an error occurs during the payment process.
     * Errors can include network failures, invalid configuration, server errors,
     * authentication failures, or other payment processing issues.
     *
     * @param error Error message or JSON string containing error details
     *              Example error formats:
     *              ```
     *              Network timeout: Unable to reach payment server
     *              or
     *              {
     *                "status": "failed",
     *                "error": "invalid_amount",
     *                "message": "Amount must be greater than zero"
     *              }
     *              ```
     *
     * @see onSamsungPaySuccess
     * @see onSamsungPayCancel
     */
    fun onSamsungPayError(error: String)

    /**
     * Called when the user cancels the payment process
     *
     * This callback is triggered when the user explicitly cancels or dismisses
     * the payment dialog/screen before completing the transaction. This is different
     * from an error - it represents a deliberate user action to abort the payment.
     *
     * **Optional callback** - default implementation does nothing
     *
     * Example usage:
     * ```kotlin
     * override fun onSamsungPayCancel() {
     *     super.onSamsungPayCancel()
     *     showMessage("Payment cancelled by user")
     *     clearPaymentUI()
     * }
     * ```
     */
    fun onSamsungPayCancel(){}
}

/**
 * Interface for monitoring application lifecycle events
 *
 * This interface provides callbacks for app foreground/background state changes.
 * Implement this interface to handle tasks that should occur when the app
 * transitions between foreground and background states.
 *
 * @see SamsungPayDataConfiguration.addAppLifeCycle
 */
interface ApplicationLifecycle {

     fun onEnterForeground()
     fun onEnterBackground()


}
