package com.jetbrains.edu.ai.debugger.core.service

import com.jetbrains.educational.ml.debugger.dto.FileContentMap
import com.jetbrains.educational.ml.debugger.dto.ProgrammingLanguage
import com.jetbrains.educational.ml.debugger.request.DebuggerHintRequestBase
import com.jetbrains.educational.ml.debugger.request.TaskDescriptionBase
import com.jetbrains.educational.ml.debugger.request.TestInfoBase

data class DebuggerHintRequest(
  override val authorSolution: FileContentMap,
  override val courseId: Int,
  override val lessonName: String,
  override val taskName: String,
  override val programmingLanguage: ProgrammingLanguage,
  override val taskDescription: TaskDescriptionBase,
  override val taskId: Int,
  override val testInfo: TestInfoBase,
  override val updateVersion: Int? = null,
  override val userSolution: FileContentMap
) : DebuggerHintRequestBase()