package company.tap.tapbenefitpay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import androidx.annotation.DimenRes
import androidx.annotation.RequiresApi
import androidx.core.os.postDelayed
import com.google.gson.Gson
import com.tap.company.samsungpay_sdk.model.ThreeDsResponse
import company.tap.tapbenefitpay.open.web_wrapper.rawFolderRefrence
import java.net.URLEncoder
import java.util.*


fun Context.px(@DimenRes dimen: Int): Int = resources.getDimension(dimen).toInt()

fun Context.dp(@DimenRes dimen: Int): Float = px(dimen) / resources.displayMetrics.density

fun Context.getDimensionsInDp(dimension: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dimension.toFloat(),
        this.resources?.displayMetrics
    ).toInt()

}

fun Context.getAssetFile(filename: String): Int {
    Log.e("packagename", this.packageName.toString())
    return resources.getIdentifier(
        filename,
        rawFolderRefrence,
        this.packageName
    )
}

fun String.getModelFromJson(): ThreeDsResponse {
    return Gson().fromJson(this, ThreeDsResponse::class.java)
}

fun getRandomNumbers(length: Int): String {
//    val allowedChars = ('A'..'Z') + ('0'..'9')
    val allowedChars = ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }.shuffled()
        .joinToString("")
}

fun getRandomTrx(length: Int=23): String {

    val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val prefix = "tck_LV"
    return prefix+(1..length)
        .map { letters.random() }.shuffled()
        .joinToString("")
}
fun <T> List<T>?.jointToStringForUrl(): String? {
    return this?.joinToString()?.replace(", ", "%22%2C%22")
}

/**
 * function to get query data and decode it
 */
@RequiresApi(Build.VERSION_CODES.O)
fun Uri.getQueryParameterFromUri(keyValue: String): String {
    val decodedBytes =
        String(Base64.getDecoder().decode(this.getQueryParameter(keyValue).toString()))

    return decodedBytes
}


 fun encodeConfigurationMapToUrl(configuraton: HashMap<String,Any>?): String? {
    val gson = Gson()
    val json: String = gson.toJson(configuraton)

    val encodedUrl = URLEncoder.encode(json, "UTF-8")
    return encodedUrl

}



fun getScreenHeight(): Int {
    return Resources.getSystem().getDisplayMetrics().heightPixels
}
fun Context.twoThirdHeightView(): Double {
    return getDeviceSpecs().first.times(2.15) / 3
}
fun Context.getDeviceSpecs(): Pair<Int, Int> {
    val displayMetrics = DisplayMetrics()
    (this.getActivity())?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
    val height = displayMetrics.heightPixels
    val width = displayMetrics.widthPixels
    val pair: Pair<Int, Int> = Pair(height, width)
    return pair
}


fun Context.getActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.getActivity()
        else -> null
    }
}
fun doAfterSpecificTime(time: Long = 1000L, execute: () -> Unit) =
    Handler(Looper.getMainLooper()).postDelayed(time) {
        execute.invoke()
    }