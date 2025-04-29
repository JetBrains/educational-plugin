package com.jetbrains.edu.ai.debugger.core.log

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun AIDebuggerLogEntry.logInfo() {
  AIDebuggerLoggerFactory.getInstance().info(this.toString())
}

fun AIDebuggerLogEntry.logError() {
  AIDebuggerLoggerFactory.getInstance().error(this.toString())
}

fun Task.toTaskData(): TaskData = TaskData(course.id, lesson.id, id)

fun List<VirtualFile>.toStringPresentation(): String = runReadAction { map { it.readText() }.toString() }
