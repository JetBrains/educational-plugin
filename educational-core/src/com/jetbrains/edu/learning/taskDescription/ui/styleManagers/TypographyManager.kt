package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.colors.FontPreferences
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.taskDescription.ui.getUISettings

internal class TypographyManager {
  private val uiSettingsFontSize =
    if (getUISettings().presentationMode) getUISettings().presentationModeFontSize else getUISettings().fontSize

  val bodyFontSize = (uiSettingsFontSize * fontScaleFactor("body.font.size")).toInt()
  val codeFontSize = (uiSettingsFontSize * fontScaleFactor("code.font.size")).toInt()
  val bodyLineHeight = (bodyFontSize * lineHeightScaleFactor("body.line.height")).toInt()
  val codeLineHeight = (codeFontSize * lineHeightScaleFactor("code.line.height")).toInt()

  val bodyFont = TaskDescriptionBundle.getOsDependentParameter(if (isJCEF()) "body.font" else "swing.body.font")
  val codeFont = TaskDescriptionBundle.getOsDependentParameter("code.font")

  private fun fontScaleFactor(parameterName: String): Float {
    val fontSizeFromProperties = TaskDescriptionBundle.getOsDependentParameter(parameterName)
    val fontSize = PropertiesComponent.getInstance().getInt(StyleManager.FONT_SIZE_PROPERTY, FontPreferences.DEFAULT_FONT_SIZE)
    return fontSizeFromProperties.toFloat() / fontSize
  }

  private fun lineHeightScaleFactor(parameterName: String): Float {
    val lineHeight = TaskDescriptionBundle.getFloatParameter(parameterName)
    val defaultValueParameterName = if (parameterName.startsWith("body")) "body.font.size" else "code.font.size"
    val fontSize = TaskDescriptionBundle.getOsDependentParameter(defaultValueParameterName)

    return (lineHeight / fontSize.toFloat())
  }
}