package net.sdfgsdfg.utils

fun isMacOS() = System.getProperty("os.name")?.lowercase()?.contains(Regex("mac|darwin")) ?: false