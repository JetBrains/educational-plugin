package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun Task.findDataSource(project: Project): LocalDataSource? {
  val url = databaseUrl(project)
  return LocalDataSourceManager.getInstance(project).dataSources.find { it.url == url }
}

fun Task.databaseUrl(project: Project): String? {
  val taskDir = getDatabaseDir(project.courseDir) ?: return null
  // Dependency on concrete database kind/SQL dialect
  return "jdbc:h2:file:${taskDir.path}/db"
}

/**
 * Return virtual file `[courseDir](/%sectionName%)?/%lessonName%/%taskName%` for the task.
 * Similar to [Task.getDir] but always returns `%taskName%` directory instead of `task` dir for framework lessons
 */
private fun Task.getDatabaseDir(courseDir: VirtualFile): VirtualFile? = lesson.getDir(courseDir)?.findChild(name)
