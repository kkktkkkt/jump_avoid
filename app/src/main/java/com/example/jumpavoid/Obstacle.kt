package com.example.jumpavoid

class Obstacle(val lane: Int, var depth: Float = 0.01f) {

    val isPassed get() = depth >= 1.05f

    fun update(speed: Float) { depth += speed }

    fun screenX(vpX: Float, roadHalfW: Float) =
        vpX + (lane - 1) * roadHalfW * depth * (2f / 3f)

    fun screenY(vpY: Float, screenH: Float) =
        vpY + depth * (screenH - vpY)

    // Half-width of obstacle rectangle at current depth
    fun halfW(roadHalfW: Float) = roadHalfW * depth / 3f
}
