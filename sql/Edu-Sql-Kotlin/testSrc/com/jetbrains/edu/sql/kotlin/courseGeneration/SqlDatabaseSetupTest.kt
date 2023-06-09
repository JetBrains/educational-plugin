package com.jetbrains.edu.sql.kotlin.courseGeneration

import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.view.DatabaseView
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sql.SqlFileType
import com.intellij.sql.psi.SqlLanguage
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.tree.TreeVisitor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilderBase.Companion.INIT_SQL
import com.jetbrains.edu.sql.jvm.gradle.findDataSource
import com.jetbrains.edu.sql.kotlin.SqlCourseGenerationTestBase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.lang.reflect.Method
import javax.swing.JTree
import javax.swing.tree.TreePath
import kotlin.test.assertNotNull

class SqlDatabaseSetupTest : SqlCourseGenerationTestBase() {

  fun `test data source creation`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/task.sql")
        }
        eduTask("task2") {
          taskFile("src/task.sql")
        }
      }
      frameworkLesson("framework_lesson2") {
        eduTask("task3") {
          taskFile("src/task.sql")
        }
        eduTask("task4") {
          taskFile("src/task.sql")
        }
      }
    }

    createCourseStructure(course)

    checkAllTasksHaveDataSource(course)
  }

  fun `test attach jdbc console`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/task.sql")
        }
        eduTask("task2") {
          taskFile("src/task.sql")
        }
      }
    }

    createCourseStructure(course)

    val fileEditorManager = FileEditorManager.getInstance(project)
    fileEditorManager.openFiles.forEach {
      checkJdbcConsoleForFile(it)
    }
    val sqlFile = findFile("lesson1/task2/src/task.sql")
    fileEditorManager.openFile(sqlFile, true)
    checkJdbcConsoleForFile(sqlFile)
  }

  fun `test attach jdbc console for framework tasks`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("src/task.sql")
        }
        eduTask("task2") {
          taskFile("src/task.sql")
          taskFile("src/task2.sql")
        }
      }
    }

    createCourseStructure(course)

    checkJdbcConsoleForFile("lesson1/task/src/task.sql")

    withVirtualFileListener(course) {
      course.findTask("lesson1", "task1").status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    checkJdbcConsoleForFile("lesson1/task/src/task.sql")
    checkJdbcConsoleForFile("lesson1/task/src/task2.sql")

    withVirtualFileListener(course) {
      testAction(PreviousTaskAction.ACTION_ID)
    }

    checkJdbcConsoleForFile("lesson1/task/src/task.sql")
  }

  fun `test database view structure`() {
    @Suppress("SqlDialectInspection")
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """create table if not exists STUDENTS_1;""")
        }
        eduTask("task2") {
          taskFile("src/task.sql")
        }
      }
      lesson("lesson2") {
        eduTask("task3") {
          taskFile("src/task.sql")
        }
        eduTask("task4") {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """create table if not exists STUDENTS_4;""")
        }
      }
      section("section1") {
        lesson("lesson3") {
          eduTask("task5") {
            taskFile("src/task.sql")
          }
          eduTask("task6") {
            taskFile("src/task.sql")
          }
        }
        lesson("lesson4") {
          eduTask("task7") {
            taskFile("src/task.sql")
          }
          eduTask("task8") {
            taskFile("src/task.sql")
          }
        }
      }
      // check special symbols,
      section("section2", customPresentableName = "section/2\\") {
        lesson("lesson5", customPresentableName = "les/s/on5") {
          eduTask("task9") {
            taskFile("src/task.sql")
          }
          eduTask("task10", customPresentableName = "/task:1:0/") {
            taskFile("src/task.sql")
          }
        }
      }
    }

    createCourseStructure(course)

    val databaseView = DatabaseView.getDatabaseView(project)
    val tree = databaseView.panel.tree

    PlatformTestUtil.waitWhileBusy(tree)
    expandImportantNodes(tree)

    PlatformTestUtil.assertTreeEqual(tree, """
      -Root Group
       -Group (lesson1) inside Root Group
        -task1: DSN
         -DB: database
          -PUBLIC: schema
           -Family of 1 table (host: schema PUBLIC)
            STUDENTS_1: table
          +Database Objects (host: database DB)
         +Server Objects (host: root <unnamed>)
        -task2: DSN
         -DB: database
          PUBLIC: schema
         +Server Objects (host: root <unnamed>)
       -Group (lesson2) inside Root Group
        -task3: DSN
         -DB: database
          PUBLIC: schema
         +Server Objects (host: root <unnamed>)
        -task4: DSN
         -DB: database
          -PUBLIC: schema
           -Family of 1 table (host: schema PUBLIC)
            STUDENTS_4: table
          +Database Objects (host: database DB)
         +Server Objects (host: root <unnamed>)
       -Group (section1) inside Root Group
        -Group (section1/lesson3) inside Group (section1) inside Root Group
         -task5: DSN
          -DB: database
           PUBLIC: schema
          +Server Objects (host: root <unnamed>)
         -task6: DSN
          -DB: database
           PUBLIC: schema
          +Server Objects (host: root <unnamed>)
        -Group (section1/lesson4) inside Group (section1) inside Root Group
         -task7: DSN
          -DB: database
           PUBLIC: schema
          +Server Objects (host: root <unnamed>)
         -task8: DSN
          -DB: database
           PUBLIC: schema
          +Server Objects (host: root <unnamed>)
       -Group (section 2\) inside Root Group
        -Group (section 2\/les s on5) inside Group (section 2\) inside Root Group
         -/task:1:0/: DSN
          -DB: database
           PUBLIC: schema
          +Server Objects (host: root <unnamed>)
         -task9: DSN
          -DB: database
           PUBLIC: schema
          +Server Objects (host: root <unnamed>)
    """.trimIndent())
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

  @Suppress("SqlDialectInspection", "SqlNoDataSourceInspection")
  fun `test database initialization`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """
            create table if not exists STUDENTS_1;
          """)
        }
        eduTask("task2") {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """
            create table if not exists STUDENTS_2;
          """)
        }
      }
    }

    createCourseStructure(course)

    checkTable(course.findTask("lesson1", "task1"), "STUDENTS_1")
    checkTable(course.findTask("lesson1", "task2"), "STUDENTS_2")
  }

  @Suppress("SqlDialectInspection", "SqlNoDataSourceInspection")
  fun `test database initialization in framework lessons`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """
            create table if not exists STUDENTS_1;
          """)
        }
        eduTask("task2") {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """
            create table if not exists STUDENTS_2;
          """)
        }
      }
    }

    createCourseStructure(course)

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    checkTable(task1, "STUDENTS_1")
    checkTable(task2, "STUDENTS_2", shouldExist = false)

    withVirtualFileListener(course) {
      // Hack to check the plugin doesn't evaluate init.sql script twice
      // If script is evaluated the second time, it will create `STUDENTS_1_1` table that test checks below
      val initSql = findFile("lesson1/task/$INIT_SQL")
      runWriteAction {
        // language=SQL
        VfsUtil.saveText(initSql, """
          create table if not exists STUDENTS_1_1;          
        """.trimIndent())
      }

      task1.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    checkTable(task2, "STUDENTS_2")

    withVirtualFileListener(course) {
      testAction(PreviousTaskAction.ACTION_ID)
    }

    checkTable(task1, "STUDENTS_1_1", shouldExist = false)
  }

  private inline fun withVirtualFileListener(course: Course, action: () -> Unit) {
    withVirtualFileListener(project, course, testRootDisposable, action)
  }

  private fun checkJdbcConsoleForFile(path: String) {
    val fileEditorManager = FileEditorManager.getInstance(project)
    val sqlFile = findFile(path)
    fileEditorManager.openFile(sqlFile, true)
    checkJdbcConsoleForFile(sqlFile)
  }

  private fun checkJdbcConsoleForFile(file: VirtualFile) {
    if (file.fileType != SqlFileType.INSTANCE) return
    // `SqlGradleStartupActivity` attaches console using `invokeLater` so we have to dispatch events in EDT
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    val console = JdbcConsoleProvider.getValidConsole(project, file)

    assertNotNull(console, "Can't find jdbc console for `$file`")

    val task = file.getTaskFile(project)?.task ?: error("Can't find task for $file")
    val taskDataSource = task.findDataSource(project) ?: error("Can't find data source for `${task.name}`")

    assertEquals("Wrong data source url", taskDataSource.url, console.dataSource.url)
  }

  companion object {

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
  }
}
