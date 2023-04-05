package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.console.runConfiguration.DatabaseScriptRunConfiguration
import com.intellij.database.console.runConfiguration.DatabaseScriptRunConfigurationOptions
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.sql.SqlFileType
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.h2.H2Dialect
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.runReadActionInSmartMode
import com.jetbrains.edu.sql.core.EduSqlBundle

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

  val taskFile = taskFiles[SqlGradleCourseBuilderBase.INIT_SQL] ?: return null
  return taskFile.getVirtualFile(project)
}
