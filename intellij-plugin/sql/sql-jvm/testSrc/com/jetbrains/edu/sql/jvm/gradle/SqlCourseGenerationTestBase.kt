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
import com.intellij.database.view.DatabaseView
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.tree.TreeVisitor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import javax.swing.JTree
import javax.swing.tree.TreePath

abstract class SqlCourseGenerationTestBase : JvmCourseGenerationTestBase() {

  // https://youtrack.jetbrains.com/issue/EDU-6933
  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
    if (ApplicationInfo.getInstance().build < BUILD_242) {
      super.runTestRunnable(testRunnable)
    }
  }

  override fun createCourseStructure(
    course: Course,
    metadata: Map<String, String>,
    waitForProjectConfiguration: Boolean
  ) {
    super.createCourseStructure(course, metadata, waitForProjectConfiguration)
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
      "Tasks ${tasks.joinToString { "`${it.pathInCourse}`" }} don't have data sources"
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
    while (RefreshQueueImpl.isRefreshInProgress) {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }

  protected fun prepareDatabaseView(): Tree {
    val databaseView = DatabaseView.getDatabaseView(project)
    val tree = databaseView.panel.getTree()

    PlatformTestUtil.waitWhileBusy(tree)
    expandImportantNodes(tree)
    return tree
  }

  protected fun checkDatabaseTree(tree: JTree, expectedWithPlaceholders: String) {
    val emptyNodeRepresentation = """
        $1-DB: database
        $1 PUBLIC: schema
        $1 +Database Objects (host: database DB)
        $1+Server Objects (host: root <unnamed>)
      """.trimIndent()

    val expected = expectedWithPlaceholders.trimIndent()
      .replace("""(\h*)$EMPTY_DATA_SOURCE_PLACEHOLDER""".toRegex(), emptyNodeRepresentation)
    PlatformTestUtil.assertTreeEqual(tree, expected)
  }

  companion object {
    @JvmStatic
    protected val EMPTY_DATA_SOURCE_PLACEHOLDER = "%EMPTY_DATA_SOURCE_PLACEHOLDER%"

    private val BUILD_242 = BuildNumber.fromString("242")!!

    private val promiseMakeVisible: Method by lazy {
      val clazz = TreeUtil::class.java
      val promiseMakeVisible = clazz.getDeclaredMethod("promiseMakeVisible", JTree::class.java, TreeVisitor::class.java, AsyncPromise::class.java)
      promiseMakeVisible.isAccessible = true
      promiseMakeVisible
    }

    // Reflection-based way to call private `TreeUtil.promiseMakeVisible(JTree, TreeVisitor, AsyncPromise<?>)`
    // It seems it's the simplest way how to reuse `TreeUtil.promiseMakeVisible` without copying a lot of code
    private fun makeTreePathsVisible(tree: JTree, visitor: TreeVisitor, promise: AsyncPromise<*>): Promise<*> {
      return promiseMakeVisible.invoke(null, tree, visitor, promise) as Promise<*>
    }

    // Similar to `TreeUtil.promiseExpand(JTree, int)` but allows custom `TreeVisitor`
    private fun expandTreePaths(tree: JTree, visitor: TreeVisitor) {
      val promise: AsyncPromise<*> = AsyncPromise<Any>()
      makeTreePathsVisible(tree, visitor, promise)
        .onError(promise::setError)
        .onSuccess {
          if (promise.isCancelled) return@onSuccess
          promise.setResult(null)
        }

      PlatformTestUtil.waitForPromise(promise)
    }

    private fun expandImportantNodes(tree: Tree) {
      fun TreePath.isRoot(): Boolean = parentPath == null
      fun TreePath.isGroupNode(): Boolean = lastPathComponent.toString().startsWith("Group")
      fun TreePath.isDbNode(): Boolean = lastPathComponent.toString().startsWith("DB: database")
      fun TreePath.isSchemaNode(): Boolean = lastPathComponent.toString().startsWith("PUBLIC: schema")
      fun TreePath.getInsidePublicSchema(): Boolean = isSchemaNode() || parentPath?.getInsidePublicSchema() == true

      expandTreePaths(tree) { path ->
        when {
          path.isRoot() -> TreeVisitor.Action.CONTINUE
          path.isGroupNode() -> TreeVisitor.Action.CONTINUE
          path.parentPath.isGroupNode() -> TreeVisitor.Action.CONTINUE
          path.isDbNode() -> TreeVisitor.Action.CONTINUE
          path.getInsidePublicSchema() -> TreeVisitor.Action.CONTINUE
          else -> TreeVisitor.Action.SKIP_CHILDREN
        }
      }
    }
  }
}
