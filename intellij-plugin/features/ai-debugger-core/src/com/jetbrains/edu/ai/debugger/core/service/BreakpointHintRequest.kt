package com.jetbrains.edu.ai.debugger.core.service

import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.dto.FileContentMap
import com.jetbrains.educational.ml.debugger.request.BreakpointHintRequestBase

data class BreakpointHintRequest(
  override val intermediateBreakpoints: List<Breakpoint>,
  override val finalBreakpoints: List<Breakpoint>,
  override val userSolution: FileContentMap
) : BreakpointHintRequestBase()