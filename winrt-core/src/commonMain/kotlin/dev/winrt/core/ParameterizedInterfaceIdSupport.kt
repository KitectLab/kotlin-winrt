package dev.winrt.core

import dev.winrt.kom.Guid

internal object ParameterizedInterfaceIdSupport {
    private val pinterfaceNamespace = guidOf("d57af411-737b-c042-abae-878b1e16adee")

    fun createFromSignature(signature: String): Guid {
        val namespaceBytes = guidToRfc4122Bytes(pinterfaceNamespace)
        val payload = namespaceBytes + signature.encodeToByteArray()
        val hash = sha1(payload).copyOf(16)
        hash[6] = ((hash[6].toInt() and 0x0F) or (5 shl 4)).toByte()
        hash[8] = ((hash[8].toInt() and 0x3F) or 0x80).toByte()
        return rfc4122BytesToGuid(hash)
    }

    private fun guidToRfc4122Bytes(guid: Guid): ByteArray {
        return byteArrayOf(
            ((guid.data1 ushr 24) and 0xFF).toByte(),
            ((guid.data1 ushr 16) and 0xFF).toByte(),
            ((guid.data1 ushr 8) and 0xFF).toByte(),
            (guid.data1 and 0xFF).toByte(),
            ((guid.data2.toInt() ushr 8) and 0xFF).toByte(),
            (guid.data2.toInt() and 0xFF).toByte(),
            ((guid.data3.toInt() ushr 8) and 0xFF).toByte(),
            (guid.data3.toInt() and 0xFF).toByte(),
            guid.data4[0],
            guid.data4[1],
            guid.data4[2],
            guid.data4[3],
            guid.data4[4],
            guid.data4[5],
            guid.data4[6],
            guid.data4[7],
        )
    }

    private fun rfc4122BytesToGuid(bytes: ByteArray): Guid {
        require(bytes.size == 16) { "GUID byte array must contain 16 bytes" }
        return Guid(
            data1 = (bytes[0].toInt() and 0xFF shl 24) or
                (bytes[1].toInt() and 0xFF shl 16) or
                (bytes[2].toInt() and 0xFF shl 8) or
                (bytes[3].toInt() and 0xFF),
            data2 = (((bytes[4].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)).toShort(),
            data3 = (((bytes[6].toInt() and 0xFF) shl 8) or (bytes[7].toInt() and 0xFF)).toShort(),
            data4 = bytes.copyOfRange(8, 16),
        )
    }

    private fun sha1(data: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476
        var h4 = 0xC3D2E1F0.toInt()

        val messageLength = data.size
        val bitLength = messageLength.toLong() * 8L
        val paddedLength = ((messageLength + 9 + 63) / 64) * 64
        val padded = ByteArray(paddedLength)
        data.copyInto(padded, endIndex = messageLength)
        padded[messageLength] = 0x80.toByte()

        var lengthIndex = paddedLength - 8
        for (shift in 7 downTo 0) {
            padded[lengthIndex++] = ((bitLength ushr (shift * 8)) and 0xFF).toByte()
        }

        val w = IntArray(80)
        var offset = 0
        while (offset < paddedLength) {
            var index = 0
            while (index < 16) {
                val base = offset + index * 4
                w[index] = ((padded[base].toInt() and 0xFF) shl 24) or
                    ((padded[base + 1].toInt() and 0xFF) shl 16) or
                    ((padded[base + 2].toInt() and 0xFF) shl 8) or
                    (padded[base + 3].toInt() and 0xFF)
                index++
            }
            while (index < 80) {
                w[index] = (w[index - 3] xor w[index - 8] xor w[index - 14] xor w[index - 16]).rotateLeft(1)
                index++
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4

            for (round in 0 until 80) {
                val (f, k) = when (round) {
                    in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                    in 20..39 -> (b xor c xor d) to 0x6ED9EBA1
                    in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                    else -> (b xor c xor d) to 0xCA62C1D6.toInt()
                }
                val temp = a.rotateLeft(5) + f + e + k + w[round]
                e = d
                d = c
                c = b.rotateLeft(30)
                b = a
                a = temp
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            offset += 64
        }

        return byteArrayOf(
            (h0 ushr 24).toByte(),
            (h0 ushr 16).toByte(),
            (h0 ushr 8).toByte(),
            h0.toByte(),
            (h1 ushr 24).toByte(),
            (h1 ushr 16).toByte(),
            (h1 ushr 8).toByte(),
            h1.toByte(),
            (h2 ushr 24).toByte(),
            (h2 ushr 16).toByte(),
            (h2 ushr 8).toByte(),
            h2.toByte(),
            (h3 ushr 24).toByte(),
            (h3 ushr 16).toByte(),
            (h3 ushr 8).toByte(),
            h3.toByte(),
            (h4 ushr 24).toByte(),
            (h4 ushr 16).toByte(),
            (h4 ushr 8).toByte(),
            h4.toByte(),
        )
    }

    private fun Int.rotateLeft(bits: Int): Int {
        return (this shl bits) or (this ushr (32 - bits))
    }
}