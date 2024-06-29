package com.jetbrains.edu.learning.ui

import com.intellij.openapi.util.IconLoader
import com.jetbrains.edu.EducationalCoreIcons
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class EduIcon @JvmOverloads constructor(val path: String, hasDark: Boolean = true) : Icon {
  private val icon = IconLoader.getIcon(path, EducationalCoreIcons::class.java)

  override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) = icon.paintIcon(c, g, x, y)

  override fun getIconWidth(): Int = icon.iconWidth

  override fun getIconHeight(): Int = icon.iconHeight
}