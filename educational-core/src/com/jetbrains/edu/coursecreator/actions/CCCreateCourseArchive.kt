package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.function.Supplier


@Suppress("ComponentNotRegistered") // educational-core.xml
class CCCreateCourseArchive
// BACKCOMPAT: 2019.3 Use lazyMessage call instead
  : CreateCourseArchiveAction(Supplier { EduCoreBundle.message("action.create.course.archive.text") }) {

  override fun showAuthorField(): Boolean = true

  override fun getArchiveCreator(project: Project, location: String): CourseArchiveCreator = EduCourseArchiveCreator(project, location)

}