package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.TaskFile

data class TemplateFileInfo(val templateName: String, val path: String, val isVisible: Boolean) {

  @JvmOverloads
  fun toTaskFile(params: Map<String, String> = emptyMap()): TaskFile? {
    val template = FileTemplateManager.getDefaultInstance().findInternalTemplate(templateName)
    if (template == null) {
      LOG.warn("Failed to obtain internal template: `$templateName`")
      return null
    }
    val taskFile = TaskFile()
    taskFile.name = path
    taskFile.setText(template.getText(params))
    taskFile.isVisible = isVisible
    return taskFile
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(TemplateFileInfo::class.java)
  }
}
