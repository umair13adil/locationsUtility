package com.embraceit.batchdrawgeolocations.utils

import android.content.Context
import java.io.IOException

/**
 * Created by umair on 2019-09-13 16:03
 * for batchDraw
 */

fun readAssetsXML(fileName: String, context: Context): String? {
    var xmlString: String? = null
    val am = context.assets
    try {
        val `is` = am.open(fileName)
        val length = `is`.available()
        val data = ByteArray(length)
        `is`.read(data)
        xmlString = String(data)
    } catch (e1: IOException) {
        e1.printStackTrace()
    } catch (e1: NullPointerException) {
        e1.printStackTrace()
    }
    return xmlString
}