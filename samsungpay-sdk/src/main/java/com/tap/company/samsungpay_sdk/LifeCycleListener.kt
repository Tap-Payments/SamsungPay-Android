package com.tap.company.samsungpay_sdk
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent



class AppLifecycleObserver : LifecycleObserver {


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onEnterForeground() {
        SamsungPayDataConfiguration.getAppLifeCycle()?.onEnterForeground()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        SamsungPayDataConfiguration.getAppLifeCycle()?.onEnterBackground()
    }
}
