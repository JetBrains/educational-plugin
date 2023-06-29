package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.actions.runDataSourceGeneralRefresh
import com.intellij.database.dataSource.DataSourceSyncManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.model.DasDataSource
import com.intellij.database.model.ObjectKind
import com.intellij.database.model.ObjectName
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.psi.DbPsiFacadeImpl
import com.intellij.database.util.DasUtil
import com.intellij.database.util.TreePattern
import com.intellij.database.util.TreePatternUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.concurrent.TimeUnit

abstract class SqlCourseGenerationTestBase : JvmCourseGenerationTestBase() {

  override fun createCourseStructure(course: Course) {
    super.createCourseStructure(course)
    waitWhileDataSourceSyncInProgress()
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }

  protected fun checkAllTasksHaveDataSource(course: Course) {
    val dataSources = LocalDataSourceManager.getInstance(project).dataSources
    val tasks = course.allTasks.toMutableSet()

    for (dataSource in dataSources) {
      // It relies on the fact that `CourseGenerationTestBase` is a heavy test, and it uses real filesystem
      val task = dataSource.task(project) ?: error("Can't find task for `${dataSource.name}` data source")
      tasks -= task
    }

    check(tasks.isEmpty()) {
      "Tasks ${tasks.joinToString { "`${it.presentableName}`" }} don't have data sources"
    }
  }

  protected fun checkTable(task: Task, tableName: String, shouldExist: Boolean = true) {
    val dataSource = task.findDataSource(project) ?: error("Can't find data source for `${task.name}`")
    val scope = TreePattern(
      TreePatternUtils.create(
        ObjectName.quoted("DB"),
        ObjectKind.DATABASE,
        TreePatternUtils.create(ObjectName.quoted("PUBLIC"), ObjectKind.SCHEMA)
      )
    )
    dataSource.introspectionScope = scope

    refreshDataSource(dataSource)

    val tables = DasUtil.getTables(dataSource as DasDataSource).toList()
    val table = tables.find { it.name.equals(tableName, ignoreCase = true) }

    if (shouldExist) {
      assertNotNull("Failed to find `$tableName` table for `${task.name}` task ", table)
    }
    else {
      assertNull("`${task.name}`'s data source shouldn't contain `$tableName` table", table)
    }
  }

  protected fun waitWhileDataSourceSyncInProgress() {
    val dataSources = LocalDataSourceManager.getInstance(project).dataSources

    while (dataSources.any { DataSourceSyncManager.getInstance().isActive(it) }) {
      Thread.sleep(10)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }

  // Approach is taken from tests of database plugin
  private fun refreshDataSource(dataSource: LocalDataSource) {
    val task = runDataSourceGeneralRefresh(project, dataSource) ?: error("Can't create refresh task")
    PlatformTestUtil.waitForFuture(task.toFuture(), TimeUnit.MINUTES.toMillis(2))
    flushDataSources(project)
  }

  // Copied from `com.intellij.database.DatabaseTestUtil`, since `DatabaseTestUtil` is not a part of IDE distribution
  private fun flushDataSources(project: Project) {
    (DbPsiFacade.getInstance(project) as DbPsiFacadeImpl).flushUpdates()
    UIUtil.dispatchAllInvocationEvents()
    waitFsSynchronizationFinished()
    UIUtil.dispatchAllInvocationEvents()
  }

  // Copied from `com.intellij.database.DatabaseTestUtil`, since `DatabaseTestUtil` is not a part of IDE distribution
  private fun waitFsSynchronizationFinished() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    UIUtil.dispatchAllInvocationEvents()
    while (RefreshQueueImpl.isRefreshInProgress()) {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }
}