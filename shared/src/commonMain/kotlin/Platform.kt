package net.sdfgsdfg

import kotlin.time.Clock

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
