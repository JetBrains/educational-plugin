package com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.laf.LafManagerImpl
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.util.ui.JBFont
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF

internal class TypographyManager {
  private val uiSettingsFontSize: Int
    get() {
      return if (UISettings.getInstance().presentationMode) {
        UISettings.getInstance().presentationModeFontSize
      }
      else {
        val fontSize = UISettings.getInstance().fontSize
        if (fontSize == 0) {
          // from com.intellij.ide.ui.AppearanceConfigurableKt#getDefaultFont
          val lafManager = LafManager.getInstance() as? LafManagerImpl
          val font = lafManager?.defaultFont ?: JBFont.label()
          font.size
        }
        else {
          fontSize
        }
      }
    }


  val bodyFontSize = (uiSettingsFontSize * fontScaleFactor("body.font.size")).toInt()
  val codeFontSize = (uiSettingsFontSize * fontScaleFactor("code.font.size")).toInt()
  val bodyLineHeight = (bodyFontSize * lineHeightScaleFactor("body.line.height")).toInt()
  val codeLineHeight = (codeFontSize * lineHeightScaleFactor("code.line.height")).toInt()

  val bodyFont = TaskToolWindowBundle.getOsDependentParameter(if (isJCEF()) "body.font" else "swing.body.font")
  val codeFont = TaskToolWindowBundle.getOsDependentParameter("code.font")

  private fun fontScaleFactor(parameterName: String): Float {
    val fontSizeFromProperties = TaskToolWindowBundle.getOsDependentParameter(parameterName)
    val fontSize = PropertiesComponent.getInstance().getInt(StyleManager.FONT_SIZE_PROPERTY, FontPreferences.DEFAULT_FONT_SIZE)
    return fontSizeFromProperties.toFloat() / fontSize
  }

  private fun lineHeightScaleFactor(parameterName: String): Float {
    val lineHeight = TaskToolWindowBundle.getFloatParameter(parameterName)
    val defaultValueParameterName = if (parameterName.startsWith("body")) "body.font.size" else "code.font.size"
    val fontSize = TaskToolWindowBundle.getOsDependentParameter(defaultValueParameterName)

    return (lineHeight / fontSize.toFloat())
  }
}