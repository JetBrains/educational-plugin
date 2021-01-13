package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.intellij.ide.ui.UISettings
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.colors.FontPreferences
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF

internal class TypographyManager {
  private val uiSettingsFontSize =
    if (UISettings.instance.presentationMode) UISettings.instance.presentationModeFontSize else UISettings.instance.fontSize

  val bodyFontSize = (uiSettingsFontSize * fontScaleFactor("body.font.size")).toInt()
  val codeFontSize = (uiSettingsFontSize * fontScaleFactor("code.font.size")).toInt()
  val bodyLineHeight = (bodyFontSize * lineHeightScaleFactor("body.line.height")).toInt()
  val codeLineHeight = (codeFontSize * lineHeightScaleFactor("code.line.height")).toInt()

  val bodyFont = TaskDescriptionBundle.getOsDependentParameter(if (isJCEF()) "body.font" else "swing.body.font")
  val codeFont = TaskDescriptionBundle.getOsDependentParameter("code.font")

  private fun fontScaleFactor(parameterName: String): Float {
    val fontSize = TaskDescriptionBundle.getOsDependentParameter(parameterName)
    val fontFactor = PropertiesComponent.getInstance().getInt(StyleManager.FONT_FACTOR_PROPERTY, FontPreferences.DEFAULT_FONT_SIZE)
    return fontSize.toFloat() / fontFactor
  }

  private fun lineHeightScaleFactor(parameterName: String): Float {
    val lineHeight = TaskDescriptionBundle.getFloatParameter(parameterName)
    val defaultValueParameterName = if (parameterName.startsWith("body")) "body.font.size" else "code.font.size"
    val fontSize = TaskDescriptionBundle.getOsDependentParameter(defaultValueParameterName)

    return (lineHeight / fontSize.toFloat())
  }
}