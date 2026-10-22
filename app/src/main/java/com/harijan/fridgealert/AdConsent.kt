package com.harijan.fridgealert

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

// 광고 동의(UMP). 유럽 등 동의가 필요한 지역에서만 동의 창이 뜨고, 한국 사용자에겐 아무것도 뜨지 않는다.
// 동의 창 문구는 AdMob 콘솔 > 개인정보 보호 및 메시지에서 만든다.
object AdConsent {
    private const val TAG = "AdConsent"
    private val adsInitialized = AtomicBoolean(false)

    // onReady(canRequestAds, privacyOptionsRequired)
    fun gather(activity: Activity, onReady: (Boolean, Boolean) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        fun finish() {
            val required = info.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            if (info.canRequestAds()) initAds(activity)
            onReady(info.canRequestAds(), required)
        }

        // 지난 실행에서 이미 동의를 받았으면 바로 광고 준비
        if (info.canRequestAds()) initAds(activity)

        info.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    error?.let { Log.w(TAG, "동의 창 오류: ${it.message}") }
                    finish()
                }
            },
            { error ->
                // 네트워크 오류 등: 이전 동의 상태 기준으로 진행
                Log.w(TAG, "동의 정보 갱신 실패: ${error.message}")
                finish()
            }
        )
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone() }
    }

    private fun initAds(activity: Activity) {
        if (adsInitialized.getAndSet(true)) return
        MobileAds.initialize(activity.applicationContext) {}
    }
}
