package com.jetbrains.edu.coursecreator.actions

import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

data class TemplateFileInfo(val templateName: String, val path: String, val isVisible: Boolean) {

  @JvmOverloads
  fun toTaskFile(params: Map<String, String> = emptyMap()): TaskFile {
    val template = GeneratorUtils.getInternalTemplateText(templateName, params)
    val taskFile = TaskFile()
    taskFile.name = path
    taskFile.text = template
    taskFile.isVisible = isVisible
    return taskFile
  }
}
