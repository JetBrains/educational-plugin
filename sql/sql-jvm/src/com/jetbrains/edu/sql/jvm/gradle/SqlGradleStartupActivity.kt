package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.autoconfig.DataSourceConfigUtil
import com.intellij.database.autoconfig.DataSourceDetector
import com.intellij.database.autoconfig.DataSourceRegistry
import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.dataSource.DatabaseAuthProviderNames
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.dataSource.validation.DatabaseDriverValidator
import com.intellij.database.model.DasDataSource
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sql.SqlFileType
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.h2.H2Dialect
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getTaskFile

class SqlGradleStartupActivity : StartupActivity.DumbAware {

  override fun runActivity(project: Project) {
    val course = project.course ?: return
    // Setup data sources only for learners for now
    if (!course.isStudy) return
    if (course.configurator !is SqlGradleConfiguratorBase) return

    if (!PropertiesComponent.getInstance(project).isTrueValue(DATABASE_SETUP_DONE)) {
      @Suppress("UnstableApiUsage")
      val dataSources = invokeAndWaitIfNeeded {
        // Dependency on concrete database kind/SQL dialect
        SqlDialectMappings.getInstance(project).setMapping(null, H2Dialect.INSTANCE)
        createDataSources(project, course)
      }
      loadDatabaseDriver(project, dataSources)
      PropertiesComponent.getInstance(project).setValue(DATABASE_SETUP_DONE, true)
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
  }

  private fun attachSqlConsoleIfNeeded(project: Project, file: VirtualFile) {
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
      if (currentConsole != null) return@invokeLater

      val session = DatabaseSessionManager.getSession(project, dataSource, task.presentableName)
      JdbcConsoleProvider.reattachConsole(project, session, file)
    }
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

  @Suppress("UnstableApiUsage")
  private fun createDataSources(project: Project, course: Course): List<LocalDataSource> {
    val dataSourceRegistry = DataSourceRegistry(project)
    val dataSources = mutableListOf<LocalDataSource>()

    // TODO: support framework lessons
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

  private fun Task.databaseUrl(project: Project): String? {
    val taskDir = getDir(project.courseDir) ?: return null
    // Dependency on concrete database kind/SQL dialect
    return "jdbc:h2:file:${taskDir.path}/db"
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

  companion object {
    private const val DATABASE_SETUP_DONE: String = "DATABASE_SETUP_DONE"
  }
}
