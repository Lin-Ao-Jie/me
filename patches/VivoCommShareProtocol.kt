package com.datanet.share.vivo

import org.json.JSONArray
import org.json.JSONObject

object VivoCommShareProtocol {
    const val DEVICE_UNKNOWN = 0
    const val DEVICE_PHONE = 1
    const val DEVICE_PAD = 2

    const val CAP_NONE = 0
    const val CAP_CALL = 1
    const val CAP_SMS = 2
    const val CAP_DATA = 4

    const val CONN_UNKNOWN = 0
    const val CONN_BR = 1
    const val CONN_P2P = 2

    data class DeviceInfo(
        val version: String,
        val deviceType: Int,
        val deviceId: String,
        val deviceName: String,
        val openId: String = "",
        val connTypeBitmask: Int = 0,
        val p2pIfName: String = "",
        val p2pIp: String = "",
        val capabilities: Int = CAP_NONE
    )

    data class StartRequest(
        val force: Boolean = false,
        val willPreemptedDeviceId: String = "",
        val targetSource: Int = 0,
        val desc: String = ""
    ) {
        fun toJson(): String = JSONObject()
            .put("fSt", if (force) 1 else 0)
            .put("wPptD", willPreemptedDeviceId)
            .put("tarS", targetSource)
            .put("desc", desc)
            .toString()
    }

    data class StopRequest(
        val force: Boolean = false,
        val targetSource: Int = 0,
        val desc: String = ""
    ) {
        fun toJson(): String = JSONObject()
            .put("fSp", if (force) 1 else 0)
            .put("tarS", targetSource)
            .put("desc", desc)
            .toString()
    }

    data class Response(
        val errorCode: Int = 0,
        val description: String = "",
        val preferredDevices: Map<String, String> = emptyMap()
    ) {
        fun toJson(): String {
            val out = JSONObject().put("err", errorCode)
            if (description.isNotEmpty()) out.put("desc", description)
            if (preferredDevices.isNotEmpty()) {
                val arr = JSONArray()
                preferredDevices.forEach { (id, name) ->
                    arr.put(JSONObject().put("id", id).put("name", name))
                }
                out.put("pDevs", arr)
            }
            return out.toString()
        }

        companion object {
            fun fromJson(value: String): Response {
                val json = JSONObject(value)
                val devices = linkedMapOf<String, String>()
                json.optJSONArray("pDevs")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        devices[item.optString("id")] = item.optString("name")
                    }
                }
                return Response(
                    json.optInt("err", 0),
                    json.optString("desc", ""),
                    devices
                )
            }
        }
    }

    fun capabilitiesToString(mask: Int): String = buildList {
        if (mask and CAP_CALL != 0) add("CALL")
        if (mask and CAP_SMS != 0) add("SMS")
        if (mask and CAP_DATA != 0) add("DATA")
    }.ifEmpty { listOf("NONE") }.joinToString("|")

    fun connectionTypesToString(mask: Int): String = buildList {
        if (mask and CONN_BR != 0) add("BR")
        if (mask and CONN_P2P != 0) add("P2P")
    }.ifEmpty { listOf("UNKNOWN") }.joinToString("|")
}
