package com.jetbrains.edu.ai.debugger.jvm

import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import com.jetbrains.edu.ai.debugger.core.ui.AIDebuggerIcons
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import javax.swing.Icon

class JvmAIDebuggerBreakpointType : JavaLineBreakpointType("jvm-line-ai", EduAIDebuggerCoreBundle.message("ai.debugger.jvm.breakpoint.description")) {
  override fun getEnabledIcon(): Icon = AIDebuggerIcons.AIBug

  override fun getGeneralDescription(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>?): String? = null

  override fun createJavaBreakpoint(
    project: Project?,
    breakpoint: XBreakpoint<JavaLineBreakpointProperties>?
  ): Breakpoint<JavaLineBreakpointProperties> {
    return JvmAIDebuggerLineBreakpoint(project, breakpoint)
  }
}
