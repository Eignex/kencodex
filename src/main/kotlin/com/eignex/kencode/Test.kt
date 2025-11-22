package com.eignex.kencode

import kotlinx.serialization.*

@Serializable
data class Payload(
    @VarInt
    val id: Int,
    @VarUInt
    @VarInt
    val delta: Int,
    val flag1: Boolean,
    val flag2: Boolean,
    val flag3: Boolean,
)

fun main() {
    val bytes = BitPackedFormat.encodeToByteArray(
        Payload(
            123, -2, true, false, true
        )
    )
    println(bytes)
    println(BitPackedFormat.decodeFromByteArray<Payload>(bytes))
}
