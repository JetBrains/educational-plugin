package com.jetbrains.edu.learning.ui

import com.intellij.ui.JBColor
import java.awt.Color

object EduColors {
  val errorTextForeground: Color = JBColor(0xac0013, 0xef5f65)
  val warningTextForeground: Color = JBColor(0xa49152, 0xbbb529)
  val correctLabelForeground: Color = JBColor.namedColor("Submissions.label.correctForeground", 0x368746, 0x499C54)
  // BACKCOMPAT: 2019.3 Use UIUtil.getErrorForeground() instead
  val wrongLabelForeground: Color = JBColor.namedColor("Submissions.label.wrongForeground", 0xC7222D, 0xFF5261)
  val hyperlinkColor: Color = JBColor.namedColor("Hyperlink.foreground", 0x6894C6, 0x5C84C9)
}
