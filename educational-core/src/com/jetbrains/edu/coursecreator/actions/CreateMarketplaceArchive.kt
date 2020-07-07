package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import java.io.File


@Suppress("ComponentNotRegistered") // educational-core.xml
class CreateMarketplaceArchive : CreateCourseArchiveAction(EduUtils.addMnemonic(EduCoreActionBundle.message("action.create.course.archive.marketplace"))) {

  override fun showAuthorField(): Boolean = false

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = project != null && isCourseCreator(project) && isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE)
  }

  override fun getArchiveCreator(project: Project, zipFile: File): CourseArchiveCreator = MarketplaceArchiveCreator(project, zipFile)

}