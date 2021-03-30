package com.jetbrains.edu.learning.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color

object EduColors {
  val errorTextForeground: Color = JBColor(0xac0013, 0xef5f65)
  val warningTextForeground: Color = JBColor(0xa49152, 0xbbb529)
  val correctLabelForeground: Color = JBColor.namedColor("Submissions.CorrectLabel.foreground", 0x368746, 0x499C54)
  val wrongLabelForeground: Color = UIUtil.getErrorForeground()
  val hyperlinkColor: Color = JBColor.namedColor("Edu.Hyperlink.foreground", 0x6894C6, 0x5C84C9)
}
