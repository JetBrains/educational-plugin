package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.util.SystemInfo

const val BODY_FONT_SIZE_MAC = 13.0
const val BODY_FONT_SIZE = 14.0
fun fontScaleMac() = BODY_FONT_SIZE_MAC / FontPreferences.DEFAULT_FONT_SIZE
fun fontScale() = BODY_FONT_SIZE / FontPreferences.DEFAULT_FONT_SIZE

const val CODE_FONT_SIZE_MAC = 13.0
const val CODE_FONT_SIZE = 14.0
fun codeFontScaleMac() = CODE_FONT_SIZE_MAC / FontPreferences.DEFAULT_FONT_SIZE
fun codeFontScale() = CODE_FONT_SIZE / FontPreferences.DEFAULT_FONT_SIZE

const val LINE_HEIGHT_MAC = 20.0
const val LINE_HEIGHT = 24.0
const val LINE_HEIGHT_SCALE_MAC = LINE_HEIGHT_MAC / BODY_FONT_SIZE_MAC
const val LINE_HEIGHT_SCALE = LINE_HEIGHT / BODY_FONT_SIZE

const val CODE_LINE_HEIGHT_MAC = 16.0
const val CODE_LINE_HEIGHT = 20.0
const val CODE_LINE_HEIGHT_SCALE_MAC = CODE_LINE_HEIGHT_MAC / CODE_FONT_SIZE_MAC
const val CODE_LINE_HEIGHT_SCALE = CODE_LINE_HEIGHT / CODE_FONT_SIZE

fun bodyFontSize() = (EditorColorsManager.getInstance().globalScheme.editorFontSize * if (SystemInfo.isMac) fontScaleMac() else fontScale()).toInt()
fun codeFontSize() = (EditorColorsManager.getInstance().globalScheme.editorFontSize * if (SystemInfo.isMac) codeFontScaleMac() else codeFontScale()).toInt()

fun bodyLineHeight() = (EditorColorsManager.getInstance().globalScheme.editorFontSize * if (SystemInfo.isMac) LINE_HEIGHT_SCALE_MAC else LINE_HEIGHT_SCALE).toInt()
fun codeLineHeight() = (EditorColorsManager.getInstance().globalScheme.editorFontSize * if (SystemInfo.isMac) CODE_LINE_HEIGHT_SCALE_MAC else CODE_LINE_HEIGHT_SCALE).toInt()