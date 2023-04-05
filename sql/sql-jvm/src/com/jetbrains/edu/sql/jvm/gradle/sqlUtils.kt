package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sql.SqlFileType
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getTaskFile

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


/**
 * Attaches sql console to given sql [file]
 */
fun attachSqlConsoleIfNeeded(project: Project, file: VirtualFile) {
  if (file.fileType != SqlFileType.INSTANCE) return

  val task = file.getTaskFile(project)?.task ?: return
  val url = task.databaseUrl(project) ?: return
  val dataSource = LocalDataSourceManager.getInstance(project).dataSources.find { it.url == url } ?: return
  // `DatabaseStartupActivity` also installs `FileEditorManagerListener` to restore jdbc console for a file.
  // Since order of listeners is not specified, it's possible to attach console from our side,
  // and listener from `DatabaseStartupActivity` will attach this console again (probably, it's a bug database plugin),
  // that leads to unexpected exceptions and behaviour.
  // So let's postpone console attaching to avoid described situation
  invokeLater {
    if (project.isDisposed) return@invokeLater
    val currentConsole = JdbcConsoleProvider.getValidConsole(project, file)
    if (currentConsole?.dataSource == dataSource) return@invokeLater

    val session = DatabaseSessionManager.getSession(project, dataSource, task.presentableName)
    JdbcConsoleProvider.reattachConsole(project, session, file)
  }
}
