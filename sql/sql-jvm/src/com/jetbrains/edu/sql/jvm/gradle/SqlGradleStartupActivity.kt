package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.validation.DatabaseDriverValidator
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.h2.H2Dialect
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import org.jetbrains.annotations.TestOnly

class SqlGradleStartupActivity : StartupActivity.DumbAware {

  override fun runActivity(project: Project) {
    if (disable) return
    val course = project.course ?: return
    // Setup data sources only for learners for now
    if (!course.isStudy) return
    if (course.configurator !is SqlGradleConfigurator) return

    val initializationState = SqlInitializationState.getInstance(project)
    if (!initializationState.dataSourceInitialized && !disable) {
      @Suppress("UnstableApiUsage")
      val dataSources = invokeAndWaitIfNeeded {
        // Dependency on concrete database kind/SQL dialect
        SqlDialectMappings.getInstance(project).setMapping(null, H2Dialect.INSTANCE)
        createDataSources(project, course.allTasks)
      }
      loadDatabaseDriver(project, dataSources)
      initializationState.dataSourceInitialized = true
    }

    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        attachSqlConsoleIfNeeded(project, file)
      }
    })

    attachSqlConsoleForOpenFiles(project)
    executeInitScripts(project, course.allTasks)
  }

  private fun loadDatabaseDriver(project: Project, dataSources: List<LocalDataSource>) {
    val dataSource = dataSources.firstOrNull() ?: return

    val downloadTask = DatabaseDriverValidator.createDownloaderTask(dataSource, null)
    object : com.intellij.openapi.progress.Task.Backgroundable(project, downloadTask.name, true) {
      override fun run(indicator: ProgressIndicator) {
        downloadTask.run(indicator)
      }
    }.queue()
  }

  companion object {

    @Volatile
    private var disable = false

    @TestOnly
    fun disable(disposable: Disposable) {
      disable = true
      Disposer.register(disposable) {
        disable = false
      }
    }
  }
}
