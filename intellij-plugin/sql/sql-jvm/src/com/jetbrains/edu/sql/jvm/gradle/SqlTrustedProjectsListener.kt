package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.ide.trustedProjects.TrustedProjectsListener
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks

/**
 * Executes init scripts for SQL courses when the project is trusted.
 *
 * Init script execution is skipped for untrusted projects,
 * so after the project is trusted, we want to finish data source initialization.
 */
class SqlTrustedProjectsListener : TrustedProjectsListener {

  override fun onProjectTrusted(project: Project) {
    val course = project.course ?: return
    if (!course.isSqlCourse) return

    executeInitScripts(project, course.allTasks)
  }
}
