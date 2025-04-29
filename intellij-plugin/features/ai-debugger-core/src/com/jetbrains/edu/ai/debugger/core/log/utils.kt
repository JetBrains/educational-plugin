package com.jetbrains.edu.ai.debugger.core.log

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun AIDebuggingLogEntry.logInfo() {
  Logger.aiDebuggingLogger.info(this.toString())
}

fun AIDebuggingLogEntry.logError() {
  Logger.aiDebuggingLogger.error(this.toString())
}

fun Task.toTaskData(): TaskData = TaskData(course.id, lesson.id, id)

fun List<VirtualFile>.toStringPresentation(): String = map { it.readText() }.toString()
