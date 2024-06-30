package com.jetbrains.edu.learning.ui

import com.intellij.openapi.util.IconLoader
import com.jetbrains.edu.EducationalCoreIcons
import java.awt.Component
import java.awt.Graphics
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * Represents an educational icon that implements the [Icon] interface.
 *
 * This class uses the IconLoader to fetch an icon from the provided path.
 *
 * @property path The path to the icon file.
 * @property darkPath The path to the icon file in dark theme.
 * @property expuiPath The path to the ExpUI version of the icon file.
 * @property expuiDarkPath The path to the ExpUI icon file in dark theme.
 */
class EduIcon private constructor(val path: String) : Icon {
  private val icon = IconLoader.getIcon(path, EducationalCoreIcons::class.java)

  override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) = icon.paintIcon(c, g, x, y)

  override fun getIconWidth(): Int = icon.iconWidth

  override fun getIconHeight(): Int = icon.iconHeight

  /**
   * Retrieves the path for the icon file in dark theme.
   */
  val darkPath: String
    get() = path.toDarkPath()

  /**
   * Retrieves the path for the ExpUI version of the icon.
   */
  val expuiPath: String
    get() {
      val pathPrefix = "/icons/com/jetbrains/edu"
      if (!path.startsWith(pathPrefix)) {
        error("Unexpected icon path ($path): should start with `$pathPrefix`")
      }
      val relativeIconPath = path.substringAfter(pathPrefix)
      return "$pathPrefix/expui$relativeIconPath"
    }

  /**
   * Retrieves the path for the ExpUI icon file in dark theme.
   */
  val expuiDarkPath: String
    get() = expuiPath.toDarkPath()

  companion object {
    /**
     * Converts a given path to its dark theme version.
     *
     * @return The dark theme path.
     */
    fun String.toDarkPath(): String {
      val path = Path.of(this)
      val nameWithoutExtension = path.nameWithoutExtension
      val extension = path.extension
      return substringBeforeLast(nameWithoutExtension) + "${nameWithoutExtension}_dark.${extension}"
    }

    /**
     * Returns an instance of [EduIcon] for the given path.
     *
     * @param path The path to the icon file.
     * @return An instance of EduIcon.
     */
    @JvmStatic
    fun get(path: String): EduIcon = EduIcon(path)

    /**
     * Annotation to indicate that a field should not have a legacy version.
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class NoLegacyVersion

    /**
     * Annotation to indicate that a field should not have an icon in dark theme.
     *
     * @property value The target for which the field should not have an icon in dark theme.
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class NoDarkTheme(val value: IconTarget = IconTarget.LEGACY)

    /**
     * Represents the target for which an icon is intended.
     */
    enum class IconTarget {
      LEGACY,
      NEW_UI,
      BOTH
    }

    /**
     * Annotation that can be applied to a field to specify a custom mapping for the ExpUI icon path.
     *
     * @property values The custom mapping for the ExpUI icon path.
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class CustomExpUIMapping(val values: String)
  }
}