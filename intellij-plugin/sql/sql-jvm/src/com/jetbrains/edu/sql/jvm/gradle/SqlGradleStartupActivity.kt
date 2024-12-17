package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.sql.dialects.SqlDialectMappings
import com.intellij.sql.dialects.h2.H2Dialect
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.sql.core.EduSqlBundle
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly

class SqlGradleStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    if (disable) return
    val course = project.course ?: return
    // Setup data sources only for learners for now
    if (!course.isStudy) return
    if (course.configurator !is SqlGradleConfigurator) return

    val initializationState = SqlInitializationState.getInstance(project)
    if (!initializationState.dataSourceInitialized && !disable) {
      loadDatabaseDriver(project)
      @Suppress("UnstableApiUsage")
      invokeAndWaitIfNeeded {
        // Dependency on concrete database kind/SQL dialect
        SqlDialectMappings.getInstance(project).setMapping(null, H2Dialect.INSTANCE)
        createDataSources(project, course.allTasks)
      }
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

  private fun loadDatabaseDriver(project: Project) {
    // Dependency on concrete database kind/SQL dialect
    val driver = DatabaseDriverManager.getInstance().getDriver(H2_DRIVER_ID)
    if (driver == null) {
      LOG.warn("Can't find H2 driver by `$H2_DRIVER_ID` id")
      return
    }

    runBlocking {
      withBackgroundProgress(project, EduSqlBundle.message("edu.sql.downloading.driver.files.progress.title"), false) {
        driver.loadArtifacts(project)
      }
    }
  }

  companion object {

    private const val H2_DRIVER_ID = "h2.unified"

    private val LOG: Logger = logger<SqlGradleStartupActivity>()

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
