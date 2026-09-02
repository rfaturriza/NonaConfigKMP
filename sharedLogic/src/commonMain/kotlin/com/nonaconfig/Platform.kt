package com.nonaconfig

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform