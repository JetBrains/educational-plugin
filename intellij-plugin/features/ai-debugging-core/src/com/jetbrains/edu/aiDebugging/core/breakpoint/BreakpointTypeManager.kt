package com.jetbrains.edu.aiDebugging.core.breakpoint

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType

interface BreakpointTypeManager {
  fun getBreakPointType(): XLineBreakpointType<XBreakpointProperties<*>>

  companion object {
    private val EP_NAME = LanguageExtension<BreakpointTypeManager>("aiDebugging.breakpointTypeManager")

    fun getInstance(language: Language): BreakpointTypeManager = EP_NAME.forLanguage(language)
  }
}
