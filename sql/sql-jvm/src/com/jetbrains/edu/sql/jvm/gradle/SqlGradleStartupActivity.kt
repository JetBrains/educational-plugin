package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.autoconfig.DataSourceConfigUtil
import com.intellij.database.autoconfig.DataSourceDetector
import com.intellij.database.autoconfig.DataSourceRegistry
import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.console.runConfiguration.DatabaseScriptRunConfiguration
import com.intellij.database.console.runConfiguration.DatabaseScriptRunConfigurationOptions
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.dataSource.DatabaseAuthProviderNames
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.dataSource.validation.DatabaseDriverValidator
import com.intellij.database.model.DasDataSource
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.sql.SqlFileType
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.h2.H2Dialect
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.runReadActionInSmartMode
import com.jetbrains.edu.sql.core.EduSqlBundle

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

    // `runWhenSmart` here is needed mostly not to `Initialize tasks databases` show progress during indexing
    DumbService.getInstance(project).runWhenSmart {
      setSqlMappingForInitScripts(course, project)

      runBackgroundableTask(EduSqlBundle.message("edu.sql.initialize.databases.progress.title"), project, false) {
        val configurations = runReadActionInSmartMode(project) {
          collectInitializeConfigurations(project, course)
        }

        CheckUtils.executeRunConfigurations(project, configurations, it)
      }
    }
  }

  // Workaround not to fail on `Application#assertReadAccessAllowed` during init script execution.
  //
  // Currently, init script execution may happen before
  // `PushedFilePropertiesUpdater` updates all file properties (including sql dialect).
  // In such cases, execution of `DatabaseScriptRunConfiguration` for init scripts
  // will requires read action to get init file language.
  // But currently, the corresponding code in database plugin is executed without read action
  // and as a result, it may lead to triggered `assertReadAccessAllowed` assertion.
  //
  // Here we propagate sql dialect forcibly which helps
  // to avoid necessity of read action during run configuration execution
  private fun setSqlMappingForInitScripts(course: Course, project: Project) {
    course.visitTasks {
      val initSql = it.findInitSqlFile(project) ?: return@visitTasks
      // Dependency on concrete database kind/SQL dialect
      SqlDialectMappings.getInstance(project).setMapping(initSql, H2Dialect.INSTANCE)
    }
  }

  private fun collectInitializeConfigurations(project: Project, course: Course): List<RunnerAndConfigurationSettings> {
    val databaseConfigurations = mutableListOf<RunnerAndConfigurationSettings>()

    course.visitTasks {
      databaseConfigurations.addIfNotNull(it.createInitializeConfiguration(project))
    }
    return databaseConfigurations
  }

  private fun Task.createInitializeConfiguration(project: Project): RunnerAndConfigurationSettings? {
    val file = findInitSqlFile(project) ?: return null
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    val dataSource = findDataSource(project) ?: return null
    val configurationsFromContext = ConfigurationContext(psiFile).configurationsFromContext.orEmpty()
    // @formatter:off
    val configurationSettings = configurationsFromContext
      .firstOrNull { it.configuration is DatabaseScriptRunConfiguration }
      ?.configurationSettings
      ?: return null
    // @formatter:on

    val target = DatabaseScriptRunConfigurationOptions.Target(dataSource.uniqueId, null)
    // Safe cast because configuration was checked before
    (configurationSettings.configuration as DatabaseScriptRunConfiguration).options.targets.add(target)
    configurationSettings.isActivateToolWindowBeforeRun = false

    return configurationSettings
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

  private fun Task.findInitSqlFile(project: Project): VirtualFile? {
    val taskFile = taskFiles[SqlGradleCourseBuilderBase.INIT_SQL] ?: return null
    return taskFile.getVirtualFile(project)
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
