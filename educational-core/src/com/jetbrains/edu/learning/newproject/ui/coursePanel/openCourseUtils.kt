package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage

fun Course.openCourse() {
  val coursesStorage = CoursesStorage.getInstance()
  val coursePath = coursesStorage.getCoursePath(this) ?: return
  val project = ProjectUtil.openProject(coursePath, null, true)
  ProjectUtil.focusProjectWindow(project, true)
}

fun showNoCourseDialog(coursePath: String, cancelButtonText: String): Int {
  return Messages.showDialog(null,
                             EduCoreBundle.message("course.dialog.course.not.found.text", FileUtil.toSystemDependentName(coursePath)),
                             EduCoreBundle.message("course.dialog.course.not.found.title"),
                             arrayOf(Messages.getOkButton(), cancelButtonText),
                             Messages.OK,
                             Messages.getErrorIcon())
}