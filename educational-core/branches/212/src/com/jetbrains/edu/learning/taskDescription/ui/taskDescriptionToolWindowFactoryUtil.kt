package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ui.GotItTooltip
import java.awt.Component
import java.awt.Point

@Suppress("UNUSED_PARAMETER")
fun getPointProvider(): (Component, Any) -> Point = GotItTooltip.BOTTOM_LEFT