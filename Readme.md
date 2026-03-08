# Samsung Pay Android Integration Guide

This guide demonstrates how to integrate the Samsung Pay SDK into your Android application using the `MainActivity` implementation as an example.

## Overview

The Samsung Pay Android SDK enables you to process Samsung Pay transactions directly in your app. This guide covers the complete integration setup, configuration, and implementation details based on the main activity code.

---

## 🎥 Demo Video - How Samsung Pay Works

### YouTube Video Demo

Click the video below to see Samsung Pay integration in action:

[![Samsung Pay Android Demo](https://github.com/Tap-Payments/SamsungPay-Android/blob/main/samsungpaydemo.mov)](https://github.com/Tap-Payments/SamsungPay-Android/blob/main/samsungpaydemo.mov)

**Demo Flow:**
1. 📱 App initializes with Samsung Pay button
2. 👆 User clicks the payment button
3. 💳 Payment dialog appears
4. ⏳ Payment is being processed
5. ✅ Transaction completes successfully
6. 📊 Success response is displayed



### Alternative: GIF Demo

```markdown
[![Samsung Pay Android Demo](samsungpaydemo.mov](https://github.com/Tap-Payments/SamsungPay-Android/blob/main/samsungpaydemo.mov)
```

---

## Table of Contents

1. [Requirements](#requirements)
2. [Installation](#installation)
3. [Configuration](#configuration)
4. [Implementation](#implementation)
5. [Callback Handling](#callback-handling)
6. [Complete Example](#complete-example)

---

## Requirements

- **Minimum SDK Version**: Android API 24+
- **Target SDK Version**: Android 33+
- **Language**: Kotlin
- **Dependencies**: Samsung Pay SDK, OkHttp3, Kotlin stdlib

---

## Installation

### Step 1: Add Repository

In your project-level `build.gradle`:

```gradle
allprojects {
    repositories {
        google()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add Dependencies

In your app-level `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.Tap-Payments:SamsungPay-Android:1.0.0'
   
}
```

### Step 3: Add Permissions

In `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Configuration

### 1. Initialize Your Activity

Create an Activity that extends `AppCompatActivity` and implements `TapSamsungPayStatusDelegate`:

```kotlin
class MainActivity : AppCompatActivity(), TapSamsungPayStatusDelegate {
    
    // Your implementation here
}
```

### 2. Define Configuration Variables

Set up the necessary configuration variables:

```kotlin
var publicKeyLive: String = "pk_test_XXXXXXXXXXXXXXXXXXXXXXXXXX"
var amount: Double = 0.1
var currency: String = "KWD"
var transactionReference: String = ""
var postUrl: String = ""
var secretString = "pk_test_XXXXXXXXXXXXXXXXXXXXXXXXXX"
```

### 3. Generate HMAC Hash String

Create a hashing utility to generate the HMAC-SHA256 hash for request validation:

```kotlin
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
```

**Usage:**

```kotlin
val stringmsg = "x_publickey${publicKeyLive}x_amount${number3digits}x_currency${currency}x_transaction${transactionReference}x_post$postUrl"
val hashString = Hmac.digest(msg = stringmsg, key = publicKeyLive)
```

---

## Implementation

### Step 1: Layout Setup

Create your activity layout (`activity_main.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/material_dynamic_secondary50">

    <com.tap.company.samsungpay_sdk.TapSamsungPay
        android:id="@+id/samsung_pay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_gravity="center_horizontal" />

    <TextView
        android:id="@+id/data"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:text="" />
</LinearLayout>
```

### Step 2: Configure the SDK

In your `onCreate()` method, call the configuration function:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    configureSdk()
    
    dataTextView = findViewById(R.id.data)
}
```

### Step 3: Build Configuration HashMap

Create a comprehensive configuration hashmap with all required parameters:

```kotlin
fun configureSdk() {
    // Operator Configuration
    val operator = HashMap<String, Any>()
    operator.put("publicKey", "pk_test_XXXXXXXXXXXXXXXXXXXXXXXXXX")
    operator.put("hashString", "")
    
    // Order Configuration
    val order = HashMap<String, Any>()
    order.put("id", "")
    order.put("amount", 0.1)
    order.put("currency", "KWD")
    
    // Merchant Configuration
    val merchant = HashMap<String, Any>()
    merchant.put("id", "")
    
    // Invoice Configuration
    val invoice = HashMap<String, Any>()
    invoice.put("id", "")
    
    // Phone Configuration
    val phone = HashMap<String, Any>()
    phone.put("countryCode", "965")
    phone.put("number", "6617090")
    
    // Contact Configuration
    val contact = HashMap<String, Any>()
    contact.put("email", "email@example.com")
    contact.put("phone", phone)
    
    // Interface Configuration
    val interface = HashMap<String, Any>()
    interface.put("locale", "en")
    interface.put("edges", "curved")
    
    // Post Configuration
    val post = HashMap<String, Any>()
    post.put("url", "")
    
    // Transaction Configuration
    val transaction = HashMap<String, Any>()
    transaction.put("amount", 1)
    transaction.put("currency", "KWD")
    
    // Reference Configuration
    val reference = HashMap<String, Any>()
    reference.put("transaction", "")
    reference.put("order", "")
    
    // Customer Name Configuration
    val name = HashMap<String, Any>()
    name.put("lang", "en")
    name.put("first", "TAP")
    name.put("middle", "")
    name.put("last", "PAYMENTS")
    
    // Customer Configuration
    val customer = HashMap<String, Any>()
    customer.put("id", "")
    customer.put("contact", contact)
    customer.put("name", listOf(name))
    
    // Complete Configuration
    val configuration = LinkedHashMap<String, Any>()
    configuration.put("paymentMethod", "samsungpay")
    configuration.put("merchant", merchant)
    configuration.put("scope", "charge")
    configuration.put("redirect", "tappaybuttonwebsdk://")
    configuration.put("customer", customer)
    configuration.put("interface", interface)
    configuration.put("reference", reference)
    configuration.put("metadata", "")
    configuration.put("post", post)
    configuration.put("order", order)
    configuration.put("operator", operator)
    configuration.put("platform", "mobile")
    configuration.put("debug", true)
    
    // Initialize the SDK
    SamsungPayConfiguration.configureWithTapSamsungPayDictionaryConfiguration(
        this,
        findViewById(R.id.samsung_pay),
        configuration,
        this
    )
}
```

---

## Callback Handling

Implement the `TapSamsungPayStatusDelegate` interface to handle various payment events:

```kotlin
override fun onSamsungPayReady() {
    super.onSamsungPayReady()
    dataTextView.text = "<<< onSamsungPayReady >>>"
    // SDK is ready to accept payments
}

override fun onSamsungPayClick() {
    super.onSamsungPayClick()
    // User clicked the payment button
}

override fun onSamsungPaySuccess(data: String) {
    dataTextView.text = "onSamsungPaySuccess >>\n$data"
    // Payment was successful
    // data contains transaction details
}

override fun onSamsungPayChargeCreated(data: String) {
    dataTextView.text = "<<<onSamsungPayChargeCreated>>>\n$data"
    // Charge was created successfully
}

override fun onSamsungPayOrderCreated(data: String) {
    dataTextView.text = "<<<onSamsungPayOrderCreated>>>\n$data"
    // Order was created successfully
}

override fun onSamsungPayCancel() {
    super.onSamsungPayCancel()
    dataTextView.text = "onSamsungPayCancel is >>\nCancelled!!!"
    // User cancelled the payment
}

override fun onSamsungPayError(error: String) {
    dataTextView.text = "Result is :: $error"
    Log.e("onError RECEIVED ::", error)
    // Handle payment errors
}
```

---

## Configuration Parameters Reference

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| **operator.publicKey** | String | Your merchant public key | `pk_test_XXXXXXXXXXXXXXXXXXXXXXXXXX` |
| **operator.hashString** | String | HMAC-SHA256 hash for validation | Generated from Hmac.digest() |
| **order.id** | String | Unique order identifier | `order_123` |
| **order.amount** | Double | Transaction amount | `0.1` |
| **order.currency** | String | Currency code | `KWD`, `USD`, `SAR` |
| **customer.name[0].lang** | String | Language code | `en`, `ar` |
| **customer.name[0].first** | String | Customer first name | `John` |
| **customer.contact.email** | String | Customer email | `customer@example.com` |
| **customer.contact.phone.countryCode** | String | Phone country code | `965`, `966` |
| **customer.contact.phone.number** | String | Phone number | `6617090` |
| **interface.locale** | String | UI language | `en`, `ar` |
| **interface.edges** | String | Button edge style | `curved`, `flat` |
| **scope** | String | Transaction scope | `charge` |
| **paymentMethod** | String | Payment method | `samsungpay` |
| **platform** | String | Platform type | `mobile` |
| **debug** | Boolean | Debug mode flag | `true`, `false` |

---

## Complete Example

Here's a complete working example combining all the pieces:

```kotlin
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

class MainActivity : AppCompatActivity(), TapSamsungPayStatusDelegate {
    
    var publicKeyLive: String = "pk_live_3zIsCFeStGLv8DNd9m054bYc"
    var amount: Double = 0.1
    var currency: String = "KWD"
    var transactionReference: String = ""
    var postUrl: String = ""
    
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
        dataTextView = findViewById(R.id.data)
    }
    
    fun configureSdk() {
        val operator = HashMap<String, Any>()
        operator.put("publicKey", publicKeyLive)
        operator.put("hashString", "")
        
        val order = HashMap<String, Any>()
        order.put("id", "")
        order.put("amount", 0.1)
        order.put("currency", "KWD")
        
        val merchant = HashMap<String, Any>()
        merchant.put("id", "")
        
        val phone = HashMap<String, Any>()
        phone.put("countryCode", "965")
        phone.put("number", "6617090")
        
        val contact = HashMap<String, Any>()
        contact.put("email", "email@example.com")
        contact.put("phone", phone)
        
        val interfacee = HashMap<String, Any>()
        interfacee.put("locale", "en")
        interfacee.put("edges", "curved")
        
        val post = HashMap<String, Any>()
        post.put("url", "")
        
        val reference = HashMap<String, Any>()
        reference.put("transaction", "")
        reference.put("order", "")
        
        val name = HashMap<String, Any>()
        name.put("lang", "en")
        name.put("first", "TAP")
        name.put("middle", "")
        name.put("last", "PAYMENTS")
        
        val customer = HashMap<String, Any>()
        customer.put("id", "")
        customer.put("contact", contact)
        customer.put("name", listOf(name))
        
        val configuration = LinkedHashMap<String, Any>()
        configuration.put("paymentMethod", "samsungpay")
        configuration.put("merchant", merchant)
        configuration.put("scope", "charge")
        configuration.put("redirect", "tappaybuttonwebsdk://")
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
    
    override fun onSamsungPayReady() {
        super.onSamsungPayReady()
        dataTextView.text = "<<< onSamsungPayReady >>>"
    }
    
    override fun onSamsungPayClick() {
        super.onSamsungPayClick()
    }
    
    override fun onSamsungPaySuccess(data: String) {
        dataTextView.text = "onSamsungPaySuccess >>\n$data\n>>>>>>>>>>>>>>>>>>>>>>>"
    }
    
    override fun onSamsungPayChargeCreated(data: String) {
        super.onSamsungPayChargeCreated(data)
        dataTextView.text = "<<<onSamsungPayChargeCreated>>>\n$data\n>>>>>>>>>>>>>>>>>>>>>>>"
    }
    
    override fun onSamsungPayOrderCreated(data: String) {
        super.onSamsungPayOrderCreated(data)
        dataTextView.text = "<<<onSamsungPayOrderCreated>>>\n$data\n>>>>>>>>>>>>>>>>>>>>>>>"
    }
    
    override fun onSamsungPayCancel() {
        super.onSamsungPayCancel()
        dataTextView.text = "onSamsungPayCancel is >>\nCancelled!!!"
    }
    
    override fun onSamsungPayError(error: String) {
        dataTextView.text = "Result is :: $error"
        Log.e("onError RECEIVED ::", error.toString())
    }
}
```

---

## Best Practices

1. **API Keys**: Always use environment-specific keys:
   - Test: `pk_test_*`
   - Production: `pk_live_*`

2. **Error Handling**: Implement proper error handling in all callback methods

3. **HMAC Generation**: Always generate HMAC server-side for production environments

4. **Data Validation**: Validate all user input before sending to the SDK

5. **Logging**: Use appropriate logging levels for debugging

6. **Testing**: Test with test keys before deploying to production

7. **User Experience**: Provide feedback to users during payment processing

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| SDK not initializing | Verify public key is correct and permissions are added |
| HMAC hash mismatch | Ensure hash string format matches server-side implementation |
| Payment fails silently | Check logs and implement error callback properly |
| Network errors | Verify internet permission and network connectivity |

---

## Support

For more information and support, visit:
- [Tap Payments Documentation](https://tap.company/)
- GitHub Repository: [Tap-Payments/SamsungPay-Android](https://github.com/Tap-Payments/SamsungPay-Android)

---

## License

This integration guide and the Samsung Pay Android SDK are provided by Tap Payments.

---

**Last Updated**: March 4, 2026
