@file:Suppress("ktlint:standard:filename")

package net.rsprot.protocol.game.outgoing.camera

@Deprecated(
    message = "Deprecated. Use CamTargetV4.",
    replaceWith = ReplaceWith("CamTargetV4"),
)
public typealias CamTarget = CamTargetV4

@Deprecated(
    message = "Deprecated. Use CamTargetV4.",
    replaceWith = ReplaceWith("CamTargetV4"),
)
public typealias CamTargetV2 = CamTargetV4

@Deprecated(
    message = "Deprecated. Use CamMoveToV3.",
    replaceWith = ReplaceWith("CamMoveToV3"),
)
public typealias CamMoveTo = CamMoveToV1

@Deprecated(
    message = "Deprecated. Use CamLookAtV3.",
    replaceWith = ReplaceWith("CamLookAtV3"),
)
public typealias CamLookAt = CamLookAtV1

@Deprecated(
    message = "Deprecated. Use CamMoveToCyclesV3.",
    replaceWith = ReplaceWith("CamMoveToCyclesV3"),
)
public typealias CamMoveToCycles = CamMoveToCyclesV1

@Deprecated(
    message = "Deprecated. Use CamRotateToCoordinateV3.",
    replaceWith = ReplaceWith("CamRotateToCoordinateV3"),
)
public typealias CamRotateToCoordinate = CamRotateToCoordinateV1

@Deprecated(
    message = "Deprecated. Use CamMoveToArcV3.",
    replaceWith = ReplaceWith("CamMoveToArcV3"),
)
public typealias CamMoveToArc = CamMoveToArcV1
