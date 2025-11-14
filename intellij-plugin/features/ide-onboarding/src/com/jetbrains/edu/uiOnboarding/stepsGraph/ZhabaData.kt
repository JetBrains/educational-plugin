package com.jetbrains.edu.uiOnboarding.stepsGraph

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.ZhabaComponent

/**
 * See [ZhabaStep] for the explanation of the data.
 */
interface ZhabaData

/**
 * Contains [ZhabaComponent], its position, and the component it is attached to.
 */
interface ZhabaDataWithComponent: ZhabaData {
  val zhabaPoint: RelativePoint
  val zhaba: ZhabaComponent
}