package com.jetbrains.edu.jbserver

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse


class CourseUpdateProjectComponent(val project: Project): ProjectComponent {

  override fun projectOpened() {

    if (project.isDisposed) return
    if (!EduUtils.isStudyProject(project)) return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return

    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      checkUpdate(course)
    }

  }

}
