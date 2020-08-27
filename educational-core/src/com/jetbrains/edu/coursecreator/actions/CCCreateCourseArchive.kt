package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle


@Suppress("ComponentNotRegistered") // educational-core.xml
class CCCreateCourseArchive : CreateCourseArchiveAction(EduUtils.addMnemonic(EduCoreBundle.message("action.create.course.archive.text"))) {

  override fun showAuthorField(): Boolean = true

  override fun getArchiveCreator(project: Project, location: String): CourseArchiveCreator = EduCourseArchiveCreator(project, location)

}