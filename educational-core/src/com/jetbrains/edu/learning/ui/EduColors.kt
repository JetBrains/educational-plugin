package com.jetbrains.edu.learning.ui

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo
import com.intellij.openapi.editor.EditorBundle
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color

object EduColors {
  val errorTextForeground: Color = JBColor.namedColor("Component.errorForeground", 0xac0013, 0xef5f65)
  val warningTextForeground: Color = JBColor.namedColor("Component.warningForeground", 0xa49152, 0xbbb529)
  val correctLabelForeground: Color = JBColor.namedColor("Submissions.CorrectLabel.foreground", 0x368746, 0x499C54)
  val navigationMapIconNotSelectedBorder: JBColor = JBColor.namedColor("NavigationMap.icon.not.selected.border", 0xC9CCD6, 0x646464)
  val navigationMapIconSelectedBorder: JBColor = JBColor.namedColor("NavigationMap.icon.selected.border", 0x3574F0, 0x3574F0)
  val navigationMapIconSolvedBorder: JBColor = JBColor.namedColor("NavigationMap.icon.solved.border", 0x369650, 0x5FAD65)

  val wrongLabelForeground: Color = UIUtil.getErrorForeground()
  val hyperlinkColor: Color get() = getColorFromThemeIfNeeded("Link.activeForeground", JBColor(0x6894C6, 0x5C84C9))

  @Suppress("SameParameterValue")
  private fun getColorFromThemeIfNeeded(colorProperty: String, customColor: Color): Color {
    val lookAndFeel = LafManager.getInstance().currentLookAndFeel
    if (lookAndFeel !is UIThemeBasedLookAndFeelInfo || lookAndFeel.name == EditorBundle.message("intellij.light.color.scheme.name")) {
      return customColor
    }
    return JBColor.namedColor(colorProperty, customColor)
  }
}
