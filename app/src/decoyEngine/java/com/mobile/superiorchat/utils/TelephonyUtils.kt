package com.mobile.superiorchat.utils

import android.content.Context
import android.telephony.TelephonyManager

object TelephonyUtils {
    fun getCarrierName(context: Context): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            var carrierName = telephonyManager?.networkOperatorName
            
            if (carrierName.isNullOrBlank()) {
                carrierName = telephonyManager?.simOperatorName
            }
            
            if (!carrierName.isNullOrBlank()) {
                carrierName
            } else {
                "Mobile Network"
            }
        } catch (e: Exception) {
            "Mobile Network"
        }
    }
}
