package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel

import com.intellij.ui.ExperimentalUI
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel

class HyperskillNotLoggedInPanel : Wrapper() {
  private val backgroundColor = JBColor.namedColor(
    "SelectCourse.Hyperskill.HyperskillNotLoggedInPanel.backgroundColor", 0xFFFFFF, 0x1E1F22
  )

  private val oldUIBackgroundColor = JBColor(0xFFFFFF,0x313335)

  init {
    val loginPanel = HyperskillTopLoginPanelWithBanner()
    val howItWorksPanel = HowItWorksPanel()

    setContent(panel {
      row {
        cell(loginPanel).align(AlignX.FILL)
      }
      row {
        cell(howItWorksPanel).align(AlignX.CENTER)
      }
    }.apply {
      isOpaque = true
      background =  if (ExperimentalUI.isNewUI()) backgroundColor else oldUIBackgroundColor
    })
  }
}



