package com.jetbrains.edu.ai.debugger.jvm

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import com.jetbrains.edu.ai.debugger.core.ui.AIDebuggerIcons
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import javax.swing.Icon

class JvmAIDebuggerBreakpointType : JavaLineBreakpointType("jvm-line-ai", EduAIDebuggerCoreBundle.message("ai.debugger.jvm.breakpoint.description")) {
  // TODO change to real image
  override fun getEnabledIcon(): Icon = AIDebuggerIcons.AIBug

  override fun getGeneralDescription(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>?): String? = null
}
