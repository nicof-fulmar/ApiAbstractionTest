package com.fulmar.tango.session.model

import java.security.PrivateKey
import java.security.interfaces.ECPublicKey

data class TangoSession(
    val myPrivateKey: PrivateKey,
    val myPublicKey: List<Byte>,
    val peerPublicKey: List<Byte>,
    val sharedKey: List<Byte>
)