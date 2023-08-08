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
import com.intellij.database.model.DasDataSource
import com.intellij.database.util.DataSourceUtil
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.sql.SqlFileType
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.h2.H2Dialect
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.sql.core.EduSqlBundle
import org.jetbrains.annotations.VisibleForTesting

fun Task.findDataSource(project: Project): LocalDataSource? {
  val url = databaseUrl(project)
  return LocalDataSourceManager.getInstance(project).dataSources.find { it.url == url }
}

fun Task.databaseUrl(project: Project): String? {
  val taskDir = getBaseTaskDir(project) ?: return null
  // Dependency on concrete database kind/SQL dialect.
  // The first `db` is just to have a separate directory for database files.
  // The second `db` is a common prefix for H2 database files
  return "jdbc:h2:file:${taskDir.path}/db/db"
}

// Heavily depends on [databaseUrl]
// Dependency on concrete database kind/SQL dialect.
private val DATA_SOURCE_URL_REGEX = "jdbc:h2:file:(?<path>.*)/db/db".toRegex()

/**
 * Returns true if a data source is created for a course task.
 * At the same time, it is possible that such data source doesn't have the corresponding task
 * because it was removed (for example, during course update)
 *
 * @see [LocalDataSource.task]
 */
fun LocalDataSource.isTaskDataSource(): Boolean {
  val url = url ?: return false
  return DATA_SOURCE_URL_REGEX.matches(url)
}

/**
 * Returns [Task] associated with the data source or <code>null</code>
 * if a data source is not associated with any task or a task doesn't exist.
 */
fun LocalDataSource.task(project: Project): Task? {
  val url = url ?: return null
  val result = DATA_SOURCE_URL_REGEX.matchEntire(url) ?: return null
  val taskPath = result.groups["path"]!!.value
  val taskDir = LocalFileSystem.getInstance().findFileByPath(taskPath) ?: return null
  return taskDir.getTask(project)
}

/**
 * Return virtual file `%courseDir%(/%sectionName%)?/%lessonName%/%taskName%` for the task.
 * Similar to [Task.getDir] but always returns `%taskName%` directory instead of `task` dir for framework lessons
 */
fun Task.getBaseTaskDir(project: Project): VirtualFile? = lesson.getDir(project.courseDir)?.findChild(name)


fun createDataSources(project: Project, tasks: List<Task>): List<LocalDataSource> {
  val dataSourceRegistry = DataSourceRegistry(project)
  dataSourceRegistry.setImportedFlag(false)
  val dataSources = mutableListOf<LocalDataSource>()
  val existingNames = LocalDataSourceManager.getInstance(project).dataSources.mapTo(HashSet()) { it.name }

  for (task in tasks) {
    val url = task.databaseUrl(project) ?: continue
    val dataSourceName = task.createDataSourceName(existingNames)
    existingNames += dataSourceName
    dataSourceRegistry.builder
      .withName(dataSourceName)
      .withGroupName(task.dataSourceGroupName)
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
    DataSourceUtil.performAutoSyncTask(project, dataSource)
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

// It's important to have unique names for each data source.
// Otherwise, `DataSourceConfigUtil.configureDetectedDataSources` (which we use to create new data sources)
// won't create a data source with already existing name (even with different `groupName`) after course update
private fun Task.createDataSourceName(existingDataSourceNames: Set<String>): String {
  val presentableName = presentableName
  var dataSourceName = presentableName
  var index = 0
  while (dataSourceName in existingDataSourceNames) {
    index++
    dataSourceName = "$presentableName ($index)"
  }
  return dataSourceName
}

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
  project.invokeLater {
    val currentConsole = JdbcConsoleProvider.getValidConsole(project, file)
    if (currentConsole?.dataSource == dataSource) return@invokeLater

    val session = DatabaseSessionManager.getSession(project, dataSource, task.presentableName)
    JdbcConsoleProvider.reattachConsole(project, session, file)
  }
}

/**
 * Attaches SQL console to already opened SQL files.
 * If [task] is specified, only console will be attached only for files belongs to the [task]
 */
fun attachSqlConsoleForOpenFiles(project: Project, task: Task? = null) {
  for (file in FileEditorManager.getInstance(project).openFiles) {
    if (task == null || file.getTaskFile(project)?.task == task) {
      attachSqlConsoleIfNeeded(project, file)
    }
  }
}

fun executeInitScripts(project: Project, tasks: List<Task>) {
  val initializationState = SqlInitializationState.getInstance(project)
  val notInitializedTasks = tasks.filter {
    val lesson = it.lesson
    // We cannot initialize database for non-current framework task since its `init.sql` file doesn't exist
    if (lesson is FrameworkLesson && lesson.currentTask() != it) return@filter false

    !initializationState.isTaskDatabaseInitialized(it)
  }

  if (notInitializedTasks.isEmpty()) return

  // `runWhenSmart` here is needed mostly not to show progress during indexing
  DumbService.getInstance(project).runWhenSmart {
    setSqlMappingForInitScripts(project, notInitializedTasks)

    InitScriptExecutionTask(project, notInitializedTasks).queue()
  }
}

private class InitScriptExecutionTask(
  private val project: Project,
  private val tasks: List<Task>
) : com.intellij.openapi.progress.Task.Backgroundable(
  project,
  EduSqlBundle.message("edu.sql.initialize.databases.progress.title"),
  false
) {
  override fun run(indicator: ProgressIndicator) {
    val configurations = runReadActionInSmartMode(project) {
      collectInitializeConfigurations(project, tasks)
    }

    CheckUtils.executeRunConfigurations(project, configurations, indicator)
  }

  override fun onSuccess() {
    val initializationState = SqlInitializationState.getInstance(project)
    for (task in tasks) {
      initializationState.taskDatabaseInitialized(task)
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
private fun setSqlMappingForInitScripts(project: Project, tasks: List<Task>) {
  for (task in tasks) {
    val initSql = task.findInitSqlFile(project) ?: continue
    // Dependency on concrete database kind/SQL dialect
    SqlDialectMappings.getInstance(project).setMapping(initSql, H2Dialect.INSTANCE)
  }
}

private fun collectInitializeConfigurations(project: Project, tasks: List<Task>): List<RunnerAndConfigurationSettings> {
  val databaseConfigurations = mutableListOf<RunnerAndConfigurationSettings>()

  for (task in tasks) {
    databaseConfigurations.addIfNotNull(task.createInitializeConfiguration(project))
  }
  return databaseConfigurations
}

private fun Task.createInitializeConfiguration(project: Project): RunnerAndConfigurationSettings? {
  val file = findInitSqlFile(project) ?: return null
  return createDatabaseScriptConfiguration(project, file)
}

@VisibleForTesting
fun Task.createDatabaseScriptConfiguration(project: Project, file: VirtualFile): RunnerAndConfigurationSettings? {
  if (file.fileType != SqlFileType.INSTANCE) return null
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

private fun Task.findInitSqlFile(project: Project): VirtualFile? {
  val lesson = lesson
  if (lesson is FrameworkLesson && lesson.currentTask() != this) return null

  val taskFile = taskFiles[SqlGradleCourseBuilder.INIT_SQL] ?: return null
  return taskFile.getVirtualFile(project)
}
