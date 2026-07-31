package com.alchitry.labs2.ui.register_interface

import com.alchitry.hardware.usb.SerialDevice

sealed class RegisterRequest {
    abstract fun process(device: SerialDevice)

    data class BasicWrite(val address: Int, val data: Int, val onComplete: (() -> Unit)? = null) : RegisterRequest() {
        override fun process(device: SerialDevice) {
            val buffer = ByteArray(9)
            buffer[0] = (1 shl 7).toByte()
            buffer[1] = (address and 0xFF).toByte()
            buffer[2] = (address shr 8 and 0xFF).toByte()
            buffer[3] = (address shr 16 and 0xFF).toByte()
            buffer[4] = (address shr 24 and 0xFF).toByte()
            buffer[5] = (data and 0xFF).toByte()
            buffer[6] = (data shr 8 and 0xFF).toByte()
            buffer[7] = (data shr 16 and 0xFF).toByte()
            buffer[8] = (data shr 24 and 0xFF).toByte()
            check(device.writeData(buffer) == buffer.size) { "Failed to write register!" }
            onComplete?.invoke()
        }
    }

    data class BasicRead(val address: Int, val onComplete: (Int?) -> Unit) : RegisterRequest() {
        override fun process(device: SerialDevice) {
            try {
                device.flushReadBuffer()
                val buffer = ByteArray(5)
                buffer[0] = (0 shl 7).toByte()
                buffer[1] = (address and 0xFF).toByte()
                buffer[2] = (address shr 8 and 0xFF).toByte()
                buffer[3] = (address shr 16 and 0xFF).toByte()
                buffer[4] = (address shr 24 and 0xFF).toByte()
                check(device.writeData(buffer) == buffer.size) { "Failed to send read request!" }
                val readBuffer = ByteArray(4)
                val bytesRead = device.readDataWithTimeout(readBuffer)
                check(bytesRead == readBuffer.size) {
                    if (readBuffer.isEmpty())
                        "Reading ${readBuffer.size} bytes took too long!"
                    else
                        "Read $bytesRead but expected ${readBuffer.size} bytes!"
                }
                onComplete(
                    (readBuffer[0].toUByte().toInt()) or (readBuffer[1].toUByte()
                        .toInt() shl 8) or (readBuffer[2].toUByte()
                        .toInt() shl 16) or (readBuffer[3].toUByte().toInt() shl 24)
                )
            } catch (e: Exception) {
                onComplete(null)
                throw e
            }
        }
    }

    data class BulkWrite(val address: Int, val increment: Boolean, val data: Collection<Int>) : RegisterRequest() {
        override fun process(device: SerialDevice) {
            TODO("Not yet implemented")
        }
    }

    data class BulkRead(val address: Int, val increment: Boolean, val length: Int) : RegisterRequest() {
        override fun process(device: SerialDevice) {
            TODO("Not yet implemented")
        }
    }
}