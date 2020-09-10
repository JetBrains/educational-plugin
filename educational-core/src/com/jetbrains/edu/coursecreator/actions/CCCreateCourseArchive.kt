package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.encrypt.EncryptionBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle


@Suppress("ComponentNotRegistered") // educational-core.xml
class CCCreateCourseArchive
  : CreateCourseArchiveAction(EduCoreBundle.lazyMessage("action.create.course.archive.text")) {

  override fun showAuthorField(): Boolean = true

  override fun getArchiveCreator(project: Project, location: String): CourseArchiveCreator =
    EduCourseArchiveCreator(project, location, EncryptionBundle.message("aesKey"))

}
