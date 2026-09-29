package com.datanet.share.common

import java.io.File

class TrafficMeter(private val interfaceMatcher: (String) -> Boolean) {
    private var lastBytes: Long = -1L
    private var lastTime: Long = -1L

    fun sampleMbps(nowMs: Long = System.currentTimeMillis()): Double? {
        val total = readBytes() ?: return null
        if (lastBytes < 0L || lastTime < 0L) {
            lastBytes = total
            lastTime = nowMs
            return null
        }
        val dt = nowMs - lastTime
        if (dt < 800L) return null
        val delta = (total - lastBytes).coerceAtLeast(0L)
        lastBytes = total
        lastTime = nowMs
        return delta * 8.0 / dt / 1000.0
    }

    private fun readBytes(): Long? = try {
        File("/proc/net/dev").useLines { lines ->
            var sum = 0L
            var found = false
            lines.forEach { line ->
                val colon = line.indexOf(':')
                if (colon <= 0) return@forEach
                val iface = line.substring(0, colon).trim()
                if (!interfaceMatcher(iface)) return@forEach
                val fields = line.substring(colon + 1).trim().split(Regex("\\s+"))
                if (fields.size >= 9) {
                    sum += (fields[0].toLongOrNull() ?: 0L) + (fields[8].toLongOrNull() ?: 0L)
                    found = true
                }
            }
            if (found) sum else null
        }
    } catch (_: Exception) { null }
}
