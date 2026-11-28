package com.tolmol.marketplace.service
interface PhotoProcessor { fun clean(bytes: ByteArray, type: String): ByteArray }
