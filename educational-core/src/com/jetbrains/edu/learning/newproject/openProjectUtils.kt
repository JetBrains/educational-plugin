@file:JvmName("OpenProjectUtils")

package com.jetbrains.edu.learning.newproject

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.projectImport.ProjectOpenedCallback
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import java.nio.file.Path

fun openNewCourseProject(course: Course, location: Path, callback: ProjectOpenedCallback): Project? {
  val task = OpenProjectTask(
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
    },
    callback = callback
  )

  return ProjectManagerEx.getInstanceEx().openProject(location, task)
}
