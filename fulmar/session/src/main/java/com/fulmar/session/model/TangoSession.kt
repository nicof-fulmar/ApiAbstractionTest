package com.fulmar.session.model

import java.security.interfaces.ECPublicKey

data class TangoSession(
    val myPublicKey: ECPublicKey,
    val peerPublicKey: ECPublicKey,
    val sharedKey: List<Byte>
)