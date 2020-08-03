package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import java.io.File


@Suppress("ComponentNotRegistered") // educational-core.xml
class CCCreateCourseArchive : CreateCourseArchiveAction(EduUtils.addMnemonic(EduCoreActionBundle.message("action.create.course.archive.text"))) {

  override fun showAuthorField(): Boolean = true

  override fun getArchiveCreator(project: Project, zipFile: File): CourseArchiveCreator = EduCourseArchiveCreator(project, zipFile)

}