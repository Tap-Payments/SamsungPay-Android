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

    fun addTapBenefitPayStatusDelegate(_tapCardStatuDelegate: TapSamsungPayStatusDelegate?) {
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

interface TapSamsungPayStatusDelegate {
    fun onSamsungPaySuccess(data: String)
    fun onSamsungPayReady(){}
    fun onSamsungPayClick(){}
    fun onSamsungPayOrderCreated(data: String){}
    fun onSamsungPayChargeCreated(data:String){}
    fun onSamsungPayError(error: String)
    fun onSamsungPayCancel(){}
}

interface ApplicationLifecycle {

     fun onEnterForeground()
     fun onEnterBackground()


}
