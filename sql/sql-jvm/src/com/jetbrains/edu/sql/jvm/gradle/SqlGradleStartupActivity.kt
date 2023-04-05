package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.autoconfig.DataSourceConfigUtil
import com.intellij.database.autoconfig.DataSourceDetector
import com.intellij.database.autoconfig.DataSourceRegistry
import com.intellij.database.dataSource.DatabaseAuthProviderNames
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.dataSource.validation.DatabaseDriverValidator
import com.intellij.database.model.DasDataSource
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.h2.H2Dialect
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class SqlGradleStartupActivity : StartupActivity.DumbAware {

  override fun runActivity(project: Project) {
    val course = project.course ?: return
    // Setup data sources only for learners for now
    if (!course.isStudy) return
    if (course.configurator !is SqlGradleConfiguratorBase) return

    val initializationState = SqlInitializationState.getInstance(project)
    if (!initializationState.dataSourceInitialized) {
      @Suppress("UnstableApiUsage")
      val dataSources = invokeAndWaitIfNeeded {
        // Dependency on concrete database kind/SQL dialect
        SqlDialectMappings.getInstance(project).setMapping(null, H2Dialect.INSTANCE)
        createDataSources(project, course)
      }
      loadDatabaseDriver(project, dataSources)
      initializationState.dataSourceInitialized = true
    }

    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        attachSqlConsoleIfNeeded(project, file)
      }
    })

    // Attach console to already opened SQL files
    for (file in FileEditorManager.getInstance(project).openFiles) {
      attachSqlConsoleIfNeeded(project, file)
    }

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

  private fun createDataSources(project: Project, course: Course): List<LocalDataSource> {
    val dataSourceRegistry = DataSourceRegistry(project)
    val dataSources = mutableListOf<LocalDataSource>()

    course.visitTasks {
      val url = it.databaseUrl(project) ?: return@visitTasks
      dataSourceRegistry.builder
        .withName(it.dataSourceName)
        .withGroupName(it.dataSourceGroupName)
        .withUrl(url)
        .withAuthProviderId(DatabaseAuthProviderNames.NO_AUTH_ID)
        .withCallback(object : DataSourceDetector.Callback() {
          override fun onCreated(dataSource: DasDataSource) {
            if (dataSource is LocalDataSource) {
              dataSources += dataSource
            }
          }
        })
        .commit()
    }
    DataSourceConfigUtil.configureDetectedDataSources(project, dataSourceRegistry, false, true, DatabaseCredentials.getInstance())

    for (dataSource in dataSources) {
      LocalDataSourceManager.getInstance(project).addDataSource(dataSource)
    }

    return dataSources
  }

  private val Task.dataSourceGroupName: String
    get() {
      val lesson = lesson
      val section = lesson.section
      return buildString {
        if (section != null) {
          append(section.presentableName.sanitizeGroupName())
          append("/")
        }
        append(lesson.presentableName.sanitizeGroupName())
      }
    }

  // Database plugin uses group name as a some path in filesystem with `/` as path separator.
  // We don't want to provide additional group inside Database View because of `/` inside section or lesson name
  // so let's replace it with ` `
  private fun String.sanitizeGroupName(): String = replace("/", " ")

  private val Task.dataSourceName: String get() = presentableName
}
