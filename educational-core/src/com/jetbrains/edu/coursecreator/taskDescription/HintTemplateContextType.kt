package com.jetbrains.edu.coursecreator.taskDescription

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.messages.EduCoreBundle

class HintTemplateContextType : TemplateContextType("EDU_TASK_DESCRIPTION_HINT",
                                                    EduCoreBundle.message("course.creator.hint.template.context")) {

  override fun isInContext(context: TemplateActionContext): Boolean {
    val project = ReadAction.compute<Project, RuntimeException> { context.file.project }
    return CCUtils.isCourseCreator(project) && EduUtilsKt.isTaskDescriptionFile(context.file.name)
  }
}
