package com.fulmar.tango.session.model

data class TangoSession(
    //val myPrivateKey: PrivateKey,
    val myPublicKey: List<Byte>,
    val peerPublicKey: List<Byte>,
    val sharedKey: List<Byte>
)