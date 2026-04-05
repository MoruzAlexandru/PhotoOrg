package com.example.photoorg

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello world, ${platform.name}!"
    }
}