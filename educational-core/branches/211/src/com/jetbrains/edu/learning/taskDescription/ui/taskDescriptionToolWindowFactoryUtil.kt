package com.jetbrains.edu.learning.taskDescription.ui

import java.awt.Component
import java.awt.Point

@Suppress("UNUSED_PARAMETER")
fun getPointProvider(): (Component) -> Point = { it -> Point(0, it.height) }