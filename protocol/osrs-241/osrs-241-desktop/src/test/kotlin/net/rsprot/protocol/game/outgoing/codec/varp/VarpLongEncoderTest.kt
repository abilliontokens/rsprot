package net.rsprot.protocol.game.outgoing.codec.varp

import io.netty.buffer.Unpooled
import net.rsprot.buffer.extensions.toJagByteBuf
import net.rsprot.crypto.cipher.NopStreamCipher
import net.rsprot.protocol.game.outgoing.varp.VarpLong
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VarpLongEncoderTest {
    /**
     * Decodes with the revision 241 client read order (rsprox `VarpLongDecoder`):
     * g4Alt3 high word, g4Alt3 low word, g2 id. A value above Int.MAX_VALUE with
     * the low word's sign bit set catches a swapped or sign-extended half.
     */
    @Test
    fun `round trips a value wider than an int`() {
        val value = 0x0000_0002_8000_0001L
        val buffer = Unpooled.buffer().toJagByteBuf()

        VarpLongEncoder().encode(NopStreamCipher, buffer, VarpLong(5753, value))

        val high = buffer.g4Alt3()
        val low = buffer.g4Alt3()
        val id = buffer.g2()
        assertEquals(5753, id)
        assertEquals(value, (high.toLong() shl 32) or (low.toLong() and 0xFFFFFFFFL))
        assertEquals(0, buffer.readableBytes())
    }
}
