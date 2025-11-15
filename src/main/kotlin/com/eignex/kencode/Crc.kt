package com.eignex.kencode

/**
 * CRC implementation for CRC-16 class algorithms.
 * Defaults parameters implement the X25 method.
 *
 * Parameters are the standard ones from CRC catalogues:
 * - poly: generator polynomial (width bits, top bit implicit)
 * - init: initial register value
 * - refin: reflect (reverse) each input byte bit order
 * - refout: reflect final CRC bits
 * - xorOut: XOR applied to final CRC value
 * - width: number of CRC bits (16 for all below)
 */
class Crc16(
    private val poly: Int = 0x8408,
    private val init: Int = 0xFFFF,
    private val refin: Boolean = true,
    private val refout: Boolean = true,
    private val xorOut: Int = 0xFFFF,
    private val width: Int = 16
) : Checksum {

    private val mask = (1 shl width) - 1
    override val size: Int = (width + 7) / 8

    override fun compute(
        data: ByteArray,
        offset: Int,
        length: Int
    ): ByteArray {
        var crc = init and mask

        for (i in 0 until length) {
            var b = data[offset + i].toInt() and 0xFF
            if (refin) {
                b = b.reverseBits8()
                crc = crc xor (b and 0xFF)
            } else {
                crc = crc xor (b shl (width - 8))
            }

            repeat(8) {
                val bit = (crc and (if (refin) 0x0001 else (1 shl (width - 1)))) != 0
                crc = if (bit) {
                    if (refin) {
                        (crc ushr 1) xor poly
                    } else {
                        ((crc shl 1) xor poly) and mask
                    }
                } else {
                    if (refin) {
                        crc ushr 1
                    } else {
                        (crc shl 1) and mask
                    }
                }
            }

            crc = crc and mask
        }

        if (refout) {
            crc = crc.reverseBits(width)
        }

        crc = crc xor xorOut
        crc = crc and mask

        // Output big-endian (high byte first).
        val out = ByteArray(size)
        for (i in 0 until size) {
            val shift = 8 * (size - 1 - i)
            out[i] = ((crc ushr shift) and 0xFF).toByte()
        }
        return out
    }
}

/**
 * CRC implementation for CRC-32 class algorithms.
 * Default parameters implement CRC-32/ISO-HDLC (a.k.a. "standard" CRC-32).
 *
 * Parameters are the standard ones from CRC catalogues:
 * - poly: generator polynomial (width bits, top bit implicit)
 * - init: initial register value
 * - refin: reflect (reverse) each input byte bit order
 * - refout: reflect final CRC bits
 * - xorOut: XOR applied to final CRC value
 * - width: number of CRC bits (32 for all below)
 */
class Crc32(
    private val poly: Int = 0xEDB88320.toInt(), // reversed 0x04C11DB7 for refin=true
    private val init: Int = 0xFFFFFFFF.toInt(),
    private val refin: Boolean = true,
    private val refout: Boolean = true,
    private val xorOut: Int = 0xFFFFFFFF.toInt(),
    private val width: Int = 32
) : Checksum {

    // Use Long internally to handle width=32 cleanly.
    private val mask: Long =
        if (width == 32) 0xFFFFFFFFL
        else (1L shl width) - 1L

    override val size: Int = (width + 7) / 8

    override fun compute(
        data: ByteArray,
        offset: Int,
        length: Int
    ): ByteArray {
        var crc = init.toLong() and mask

        for (i in 0 until length) {
            var b = data[offset + i].toInt() and 0xFF
            if (refin) {
                b = b.reverseBits8()
                crc = crc xor (b.toLong() and 0xFFL)
            } else {
                crc = crc xor ((b.toLong() and 0xFFL) shl (width - 8))
            }

            repeat(8) {
                val bitSet = if (refin) {
                    (crc and 1L) != 0L
                } else {
                    (crc and (1L shl (width - 1))) != 0L
                }

                crc = if (bitSet) {
                    if (refin) {
                        (crc ushr 1) xor (poly.toLong() and mask)
                    } else {
                        ((crc shl 1) xor (poly.toLong() and mask)) and mask
                    }
                } else {
                    if (refin) {
                        crc ushr 1
                    } else {
                        (crc shl 1) and mask
                    }
                }
            }

            crc = crc and mask
        }

        var finalCrc = crc

        if (refout) {
            // reverseBits(width) operates on Int; clamp via toInt() and then back to Long
            finalCrc = finalCrc.toInt().reverseBits(width).toLong() and mask
        }

        finalCrc = (finalCrc xor (xorOut.toLong() and mask)) and mask

        // Output big-endian (high byte first).
        val out = ByteArray(size)
        for (i in 0 until size) {
            val shift = 8 * (size - 1 - i)
            out[i] = ((finalCrc ushr shift) and 0xFFL).toByte()
        }
        return out
    }
}

/** Bit helpers */

private fun Int.reverseBits(width: Int): Int {
    var v = this
    var r = 0
    repeat(width) {
        r = (r shl 1) or (v and 1)
        v = v ushr 1
    }
    return r
}

private fun Int.reverseBits8(): Int {
    var v = this and 0xFF
    v = ((v and 0xF0) ushr 4) or ((v and 0x0F) shl 4)
    v = ((v and 0xCC) ushr 2) or ((v and 0x33) shl 2)
    v = ((v and 0xAA) ushr 1) or ((v and 0x55) shl 1)
    return v and 0xFF
}
