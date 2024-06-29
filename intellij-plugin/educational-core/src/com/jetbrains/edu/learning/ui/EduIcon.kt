package com.jetbrains.edu.learning.ui

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.taskToolWindow.isNewUI
import java.awt.Component
import java.awt.Graphics
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class EduIcon @JvmOverloads constructor(val path: String, hasDark: Boolean = true) : Icon {
  private val icon = IconLoader.getIcon(path, EducationalCoreIcons::class.java)

  override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) = icon.paintIcon(c, g, x, y)

  override fun getIconWidth(): Int = icon.iconWidth

  override fun getIconHeight(): Int = icon.iconHeight

  val expuiPath: String
    get() {
      val pathPrefix = "/icons/com/jetbrains/edu"
      if (!path.startsWith(pathPrefix)) {
        error("Unexpected icon path ($path): should start with `$pathPrefix`")
      }
      val relativeIconPath = path.substringAfterLast(pathPrefix)
      return "$pathPrefix/expui$relativeIconPath"
    }

  val expuiDarkPath: String
    get() = expuiPath.toDarkPath()

  companion object {
    private fun String.toDarkPath(): String {
      val path = Path.of(this)
      val nameWithoutExtension = path.nameWithoutExtension
      val extension = path.extension
      return substringBeforeLast(nameWithoutExtension) + "${nameWithoutExtension}_dark.${extension}"
    }
  }
}