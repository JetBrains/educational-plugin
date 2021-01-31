package com.jetbrains.edu.coursecreator.actions.marketplace

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.actions.CreateCourseArchiveAction
import com.jetbrains.edu.learning.messages.EduCoreBundle

@Suppress("ComponentNotRegistered") // Marketplace.xml
class CreateMarketplaceArchive
  : CreateCourseArchiveAction(EduCoreBundle.lazyMessage("action.create.course.archive.marketplace")) {

  override fun showAuthorField(): Boolean = false

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = project != null && isCourseCreator(project)
  }

  override fun getArchiveCreator(project: Project, location: String): CourseArchiveCreator =
    MarketplaceArchiveCreator(project, location)

}
