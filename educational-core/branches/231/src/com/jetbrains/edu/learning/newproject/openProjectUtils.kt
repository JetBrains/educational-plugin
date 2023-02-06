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
  prepareToOpenCallback: suspend (Project, Module) -> Unit
): Project? {
  val task = OpenProjectTask(course, prepareToOpenCallback)

  return ProjectManagerEx.getInstanceEx().openProject(location, task)
}

private fun OpenProjectTask(course: Course, prepareToOpenCallback: suspend (Project, Module) -> Unit): OpenProjectTask {
  @Suppress("UnstableApiUsage")
  return OpenProjectTask {
    forceOpenInNewFrame = true
    isNewProject = true
    isProjectCreatedWithWizard = false
    runConfigurators = true
    beforeInit = {
      it.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, true)
    }
    preparedToOpen = {
      StudyTaskManager.getInstance(it.project).course = course
      prepareToOpenCallback(it.project, it)
    }
  }
}
