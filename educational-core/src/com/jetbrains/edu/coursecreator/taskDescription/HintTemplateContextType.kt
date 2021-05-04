package com.jetbrains.edu.coursecreator.taskDescription

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils

class HintTemplateContextType : TemplateContextType("EDU_TASK_DESCRIPTION_HINT", "&Task description file") {

  override fun isInContext(context: TemplateActionContext): Boolean {
    val project = ReadAction.compute<Project, RuntimeException> { context.file.project }
    return CCUtils.isCourseCreator(project) && EduUtils.isTaskDescriptionFile(context.file.name)
  }
}
