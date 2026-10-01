package net.rsprot.protocol.game.outgoing.codec.playerinfo.extendedinfo

import io.netty.buffer.ByteBufAllocator
import net.rsprot.buffer.JagByteBuf
import net.rsprot.buffer.extensions.toJagByteBuf
import net.rsprot.compression.provider.HuffmanCodecProvider
import net.rsprot.protocol.internal.game.outgoing.info.encoder.PrecomputedExtendedInfoEncoder
import net.rsprot.protocol.internal.game.outgoing.info.playerinfo.extendedinfo.Appearance
import net.rsprot.protocol.internal.game.outgoing.info.playerinfo.extendedinfo.ObjTypeCustomisation

@Suppress("DuplicatedCode")
public class PlayerAppearanceEncoder : PrecomputedExtendedInfoEncoder<Appearance> {
    override fun precompute(
        alloc: ByteBufAllocator,
        huffmanCodecProvider: HuffmanCodecProvider,
        extendedInfo: Appearance,
    ): JagByteBuf {
        val intermediate = alloc.buffer(100).toJagByteBuf()
        intermediate.p1(extendedInfo.bodyType.toInt())
        intermediate.p1(extendedInfo.skullIcon.toInt())
        intermediate.p1(extendedInfo.overheadIcon.toInt())
        if (extendedInfo.transformedNpcId != UShort.MAX_VALUE) {
            pTransmog(intermediate, extendedInfo)
        } else {
            pEquipment(intermediate, extendedInfo)
        }
        pIdentKits(intermediate, extendedInfo)
        pColours(intermediate, extendedInfo)
        pBaseAnimationSet(intermediate, extendedInfo)
        intermediate.pjstr(extendedInfo.name)
        intermediate.p1(extendedInfo.combatLevel.toInt())
        intermediate.p2(extendedInfo.skillLevel.toInt())
        intermediate.p1(if (extendedInfo.hidden) 1 else 0)
        pObjTypeCustomisations(intermediate, extendedInfo)
        intermediate.pjstr(extendedInfo.beforeName)
        intermediate.pjstr(extendedInfo.afterName)
        intermediate.pjstr(extendedInfo.afterCombatLevel)
        intermediate.p1(extendedInfo.pronoun.toInt())
        val capacity = intermediate.readableBytes() + 1
        val buffer = alloc.buffer(capacity, capacity).toJagByteBuf()
        buffer.p1Alt2(capacity - 1)
        try {
            buffer.pdataAlt1(intermediate.buffer)
        } finally {
            intermediate.buffer.release()
        }
        return buffer
    }

    private fun pTransmog(
        intermediate: JagByteBuf,
        extendedInfo: Appearance,
    ) {
        intermediate.p2(-1)
        intermediate.p2(extendedInfo.transformedNpcId.toInt())
    }

    private fun buildHiddenWearposFlag(hidden: ByteArray): Int {
        var hiddenWearposFlag = 0
        for (i in 0..<12) {
            val pos = hidden[i].toInt()
            val wearpos2 = pos and 0xF
            if (wearpos2 != 0xF) {
                hiddenWearposFlag = hiddenWearposFlag or (1 shl wearpos2)
            }
            val wearpos3 = pos ushr 4 and 0xF
            if (wearpos3 != 0xF) {
                hiddenWearposFlag = hiddenWearposFlag or (1 shl wearpos3)
            }
        }
        return hiddenWearposFlag
    }

    private fun pEquipment(
        intermediate: JagByteBuf,
        extendedInfo: Appearance,
    ) {
        val identKit = extendedInfo.identKit
        val objs = extendedInfo.wornObjs
        val hiddenWearposFlag = buildHiddenWearposFlag(extendedInfo.hiddenWearPos)
        for (wearpos in 0..<12) {
            if (hiddenWearposFlag and (1 shl wearpos) != 0) {
                intermediate.p1(0)
                continue
            }
            val obj = objs[wearpos].toInt() and 0xFFFF
            if (obj != 0xFFFF) {
                intermediate.p2(obj + 0x800)
                continue
            }
            val identKitSlot = Appearance.identKitSlotList[wearpos]
            if (identKitSlot == -1) {
                intermediate.p1(0)
                continue
            }
            val identKitValue = identKit[identKitSlot].toInt() and 0xFFFF
            if (identKitValue == 0xFFFF) {
                intermediate.p1(0)
            } else {
                intermediate.p2(identKitValue + 0x100)
            }
        }
    }

    private fun pIdentKits(
        intermediate: JagByteBuf,
        extendedInfo: Appearance,
    ) {
        val identKit = extendedInfo.identKit
        for (wearpos in 0..<12) {
            val identKitSlot = Appearance.identKitSlotList[wearpos]
            if (identKitSlot == -1) {
                intermediate.p1(0)
                continue
            }
            val identKitValue = identKit[identKitSlot].toInt() and 0xFFFF
            if (identKitValue == 0xFFFF) {
                intermediate.p1(0)
            } else {
                intermediate.p2(identKitValue + 0x100)
            }
        }
    }

    private fun pColours(
        intermediate: JagByteBuf,
        extendedInfo: Appearance,
    ) {
        val colours = extendedInfo.colours
        for (i in colours.indices) {
            intermediate.p1(colours[i].toInt())
        }
    }

    private fun pBaseAnimationSet(
        intermediate: JagByteBuf,
        extendedInfo: Appearance,
    ) {
        intermediate.p2(extendedInfo.readyAnim.toInt())
        intermediate.p2(extendedInfo.turnAnim.toInt())
        intermediate.p2(extendedInfo.walkAnim.toInt())
        intermediate.p2(extendedInfo.walkAnimBack.toInt())
        intermediate.p2(extendedInfo.walkAnimLeft.toInt())
        intermediate.p2(extendedInfo.walkAnimRight.toInt())
        intermediate.p2(extendedInfo.runAnim.toInt())
    }

    private fun pObjTypeCustomisations(
        intermediate: JagByteBuf,
        extendedInfo: Appearance,
    ) {
        val marker = intermediate.writerIndex()
        intermediate.skipWrite(2)
        val objTypeCustomisations = extendedInfo.objTypeCustomisation
        var flag = 0
        for (wearpos in objTypeCustomisations.indices) {
            val objTypeCustomisation = objTypeCustomisations[wearpos] ?: continue
            pObjTypeCustomisation(intermediate, objTypeCustomisation)
            flag = flag or (1 shl (12 - wearpos))
        }
        if (extendedInfo.forceModelRefresh) flag = flag or 0x8000
        val pos = intermediate.writerIndex()
        intermediate.writerIndex(marker)
        intermediate.p2(flag)
        intermediate.writerIndex(pos)
    }

    private fun pObjTypeCustomisation(
        intermediate: JagByteBuf,
        customisation: ObjTypeCustomisation,
    ) {
        var flag = 0
        val recolours = recolours(customisation)
        if (recolours != null) {
            flag = flag or 0x1
        }
        val retextures = retextures(customisation)
        if (retextures != null) {
            flag = flag or 0x2
        }
        if (customisation.manWear != ObjTypeCustomisation.DEFAULT_MODEL ||
            customisation.womanWear != ObjTypeCustomisation.DEFAULT_MODEL
        ) {
            flag = flag or 0x4
        }
        if (customisation.manHead != ObjTypeCustomisation.DEFAULT_MODEL ||
            customisation.womanHead != ObjTypeCustomisation.DEFAULT_MODEL
        ) {
            flag = flag or 0x8
        }
        if (customisation.overrideColour != null || customisation.colour != null) {
            flag = flag or 0x10
        }
        intermediate.p1(flag)
        if (recolours != null) {
            pReplacements(intermediate, recolours)
        }
        if (retextures != null) {
            pReplacements(intermediate, retextures)
        }
        if (flag and 0x10 != 0) {
            intermediate.p1(if (customisation.overrideColour == true) 1 else 0)
            intermediate.p2(customisation.colour ?: 0)
        }
        if (flag and 0x4 != 0) {
            pObjTypeWearModels(intermediate, customisation)
        }
        if (flag and 0x8 != 0) {
            pObjTypeHeadModels(intermediate, customisation)
        }
    }

    private fun pReplacements(
        intermediate: JagByteBuf,
        replacements: List<ObjTypeCustomisation.Replacement>,
    ) {
        intermediate.p1(replacements.size)
        for (replacement in replacements) {
            intermediate.p1(replacement.index)
            intermediate.p2(replacement.value)
        }
    }

    private fun recolours(customisation: ObjTypeCustomisation): List<ObjTypeCustomisation.Replacement>? {
        customisation.recolours?.let { return it }
        val indices = customisation.recolIndices.toInt()
        if (indices == 0xFF) return null
        val recolours = ArrayList<ObjTypeCustomisation.Replacement>(2)
        val index1 = indices and 0xF
        if (index1 != 0xF) {
            recolours += ObjTypeCustomisation.Replacement(index1, customisation.recol1.toInt())
        }
        val index2 = indices ushr 4 and 0xF
        if (index2 != 0xF) {
            recolours += ObjTypeCustomisation.Replacement(index2, customisation.recol2.toInt())
        }
        return recolours.ifEmpty { null }
    }

    private fun retextures(customisation: ObjTypeCustomisation): List<ObjTypeCustomisation.Replacement>? {
        customisation.retextures?.let { return it }
        val indices = customisation.retexIndices.toInt()
        if (indices == 0xFF) return null
        val retextures = ArrayList<ObjTypeCustomisation.Replacement>(2)
        val index1 = indices and 0xF
        if (index1 != 0xF) {
            retextures += ObjTypeCustomisation.Replacement(index1, customisation.retex1.toInt())
        }
        val index2 = indices ushr 4 and 0xF
        if (index2 != 0xF) {
            retextures += ObjTypeCustomisation.Replacement(index2, customisation.retex2.toInt())
        }
        return retextures.ifEmpty { null }
    }

    private fun pObjTypeWearModels(
        intermediate: JagByteBuf,
        customisation: ObjTypeCustomisation,
    ) {
        intermediate.p4(customisation.manWear)
        intermediate.p4(customisation.womanWear)
    }

    private fun pObjTypeHeadModels(
        intermediate: JagByteBuf,
        customisation: ObjTypeCustomisation,
    ) {
        intermediate.p4(customisation.manHead)
        intermediate.p4(customisation.womanHead)
    }
}
