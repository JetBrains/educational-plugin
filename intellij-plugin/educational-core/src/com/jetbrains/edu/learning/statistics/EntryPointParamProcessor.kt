package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseParamsProcessor

private const val MAX_VALUE_LENGTH = 16

class EntryPointParamProcessor : CourseParamsProcessor<String> {
  override fun findApplicableContext(params: Map<String, String>): String? {
    val entryPointValue = params[ENTRY_POINT] ?: return null
    if (entryPointValue.length > MAX_VALUE_LENGTH) {
      thisLogger().warn("entry point value is too long: $entryPointValue. Max supported length is $MAX_VALUE_LENGTH")
      return null
    }
    return entryPointValue
  }

  override fun processCourseParams(project: Project, course: Course, context: String) {
    EntryPointManager.getInstance(project).entryPoint = context
  }

  companion object {
    const val ENTRY_POINT = "entry_point"
  }
}
