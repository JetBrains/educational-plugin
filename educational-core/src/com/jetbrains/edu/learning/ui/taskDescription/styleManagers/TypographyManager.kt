package com.jetbrains.edu.learning.ui.taskDescription.styleManagers

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.FontPreferences
import com.jetbrains.edu.learning.EduSettings

internal class TypographyManager {
  private val editorFontSize = EditorColorsManager.getInstance().globalScheme.editorFontSize

  val bodyFontSize = (editorFontSize * fontScaleFactor("body.font.size")).toInt()
  val codeFontSize = (editorFontSize * fontScaleFactor("code.font.size")).toInt()
  val bodyLineHeight = (bodyFontSize * lineHeightScaleFactor("body.line.height")).toInt()
  val codeLineHeight = (codeFontSize * lineHeightScaleFactor("code.line.height")).toInt()

  val bodyFont = TaskDescriptionBundle.getOsDependentParameter(
    if (EduSettings.getInstance().shouldUseJavaFx()) "body.font" else "swing.body.font")
  val codeFont = TaskDescriptionBundle.getOsDependentParameter("code.font")

  private fun fontScaleFactor(parameterName: String): Float {
    val fontSize = TaskDescriptionBundle.getFloatParameter(parameterName)

    return fontSize / FontPreferences.DEFAULT_FONT_SIZE
  }

  private fun lineHeightScaleFactor(parameterName: String): Float {
    val lineHeight = TaskDescriptionBundle.getFloatParameter(parameterName)
    val defaultValueParameterName = if (parameterName.startsWith("body")) "body.font.size" else "code.font.size"
    val fontSize = TaskDescriptionBundle.getFloatParameter(defaultValueParameterName)

    return (lineHeight / fontSize)
  }
}