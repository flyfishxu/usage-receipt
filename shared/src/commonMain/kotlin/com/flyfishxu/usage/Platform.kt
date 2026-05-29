package com.flyfishxu.usage

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform