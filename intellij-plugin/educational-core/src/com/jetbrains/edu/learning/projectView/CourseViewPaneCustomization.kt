package com.jetbrains.edu.learning.projectView

import com.intellij.openapi.extensions.ExtensionPointName
import javax.swing.JTree

interface CourseViewPaneCustomization {
  fun customize(tree: JTree)

  companion object {
    val EP_NAME = ExtensionPointName.create<CourseViewPaneCustomization>("Educational.courseViewPaneCustomization")
    fun customize(tree: JTree) {
      EP_NAME.computeSafeIfAny { it.customize(tree) }
    }
  }
}