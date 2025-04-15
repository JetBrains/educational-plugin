package com.jetbrains.edu.aiDebugging.jvm

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import com.jetbrains.edu.aiDebugging.core.ui.AIDebuggingIcons
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import javax.swing.Icon

class JvmAIDebuggerBreakpointType : JavaLineBreakpointType("jvm-line-ai", EduAIDebuggingCoreBundle.message("ai.debugger.jvm.breakpoint.description")) {
  // TODO change to real image
  override fun getEnabledIcon(): Icon = AIDebuggingIcons.AIBug

  override fun getGeneralDescription(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>?): String? = null
}
