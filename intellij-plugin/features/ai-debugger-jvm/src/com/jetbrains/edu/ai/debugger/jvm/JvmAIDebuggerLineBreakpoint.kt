package com.jetbrains.edu.ai.debugger.jvm

import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.jetbrains.edu.ai.debugger.core.ui.AIDebuggerIcons
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import javax.swing.Icon

class JvmAIDebuggerLineBreakpoint(
  project: Project?,
  xBreakpoint: XBreakpoint<out XBreakpointProperties<*>>?
) : LineBreakpoint<JavaLineBreakpointProperties>(project, xBreakpoint, false) {
  override fun getVerifiedIcon(isMuted: Boolean): Icon = AIDebuggerIcons.AIBug
}
