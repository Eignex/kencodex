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
    poly: Int = 0x1021,
    init: Int = 0xFFFF,
    private val refin: Boolean = true,
    private val refout: Boolean = true,
    private val xorOut: Int = 0xFFFF,
    private val width: Int = 16
) : Checksum {

    private val mask = (1 shl width) - 1

    // Internal representation is reflected (LSB-first), so pre-reflect poly and init.
    private val refPoly: Int = poly.reverseBits(width)
    private val refInit: Int = init.reverseBits(width)

    override val size: Int = (width + 7) / 8

    override fun compute(
        data: ByteArray,
        offset: Int,
        length: Int
    ): ByteArray {
        var crc = refInit and mask

        for (i in 0 until length) {
            val raw = data[offset + i].toInt() and 0xFF
            val b = if (refin) raw else raw.reverseBits8()

            crc = crc xor b
            repeat(8) {
                crc = if ((crc and 1) != 0) {
                    (crc ushr 1) xor refPoly
                } else {
                    crc ushr 1
                }
            }
            crc = crc and mask
        }

        // Convert from internal reflected form to requested output form.
        crc = if (refout) crc else crc.reverseBits(width)

        crc = (crc xor xorOut) and mask

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
    poly: Int = 0x04C11DB7, // canonical CRC-32 poly
    init: Int = 0xFFFFFFFF.toInt(),
    private val refin: Boolean = true,
    private val refout: Boolean = true,
    private val xorOut: Int = 0xFFFFFFFF.toInt(),
    private val width: Int = 32
) : Checksum {

    private val mask: Long =
        if (width == 32) 0xFFFFFFFFL
        else (1L shl width) - 1L

    // Internal representation: reflected
    private val refPoly: Long = poly.reverseBits(width).toLong() and mask
    private val refInit: Long = init.reverseBits(width).toLong() and mask

    override val size: Int = (width + 7) / 8

    override fun compute(
        data: ByteArray,
        offset: Int,
        length: Int
    ): ByteArray {
        var crc = refInit

        for (i in 0 until length) {
            val raw = data[offset + i].toInt() and 0xFF
            val b = if (refin) raw else raw.reverseBits8()

            crc = crc xor (b.toLong() and 0xFFL)
            repeat(8) {
                crc = if ((crc and 1L) != 0L) {
                    (crc ushr 1) xor refPoly
                } else {
                    crc ushr 1
                }
            }
            crc = crc and mask
        }

        var finalCrc = if (refout) {
            crc
        } else {
            crc.toInt().reverseBits(width).toLong() and mask
        }

        finalCrc = (finalCrc xor (xorOut.toLong() and mask)) and mask

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
