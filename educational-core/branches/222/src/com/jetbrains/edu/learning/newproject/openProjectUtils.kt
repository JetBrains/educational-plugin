@file:JvmName("OpenProjectUtils")

package com.jetbrains.edu.learning.newproject

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import java.nio.file.Path

fun openNewCourseProject(
  course: Course,
  location: Path,
  prepareToOpenCallback: (Project, Module) -> Unit
): Project? {
  val task = OpenProjectTask(course, prepareToOpenCallback)

  return ProjectManagerEx.getInstanceEx().openProject(location, task)
}

private fun OpenProjectTask(course: Course, prepareToOpenCallback: (Project, Module) -> Unit): OpenProjectTask {
  return OpenProjectTask(
    forceOpenInNewFrame = true,
    projectToClose = null,
    isNewProject = true,
    runConfigurators = true,
    isProjectCreatedWithWizard = true,
    beforeInit = {
      it.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, true)
    },
    preparedToOpen = {
      StudyTaskManager.getInstance(it.project).course = course
      prepareToOpenCallback(it.project, it)
    }
  )
}
