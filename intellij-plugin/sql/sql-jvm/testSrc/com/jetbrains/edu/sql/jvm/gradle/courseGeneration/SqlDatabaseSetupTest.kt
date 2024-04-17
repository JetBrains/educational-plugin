package com.jetbrains.edu.sql.jvm.gradle.courseGeneration

import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sql.SqlFileType
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.withVirtualFileListener
import com.jetbrains.edu.sql.jvm.gradle.SqlCourseGenerationTestBase
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilder.Companion.INIT_SQL
import com.jetbrains.edu.sql.jvm.gradle.findDataSource
import com.jetbrains.edu.sql.jvm.gradle.sqlCourse
import org.junit.Test
import kotlin.test.assertNotNull

class SqlDatabaseSetupTest : SqlCourseGenerationTestBase() {

  @Test
  fun `test data source creation`() {
    val course = sqlCourse {
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

  @Test
  fun `test attach jdbc console`() {
    val course = sqlCourse {
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

  @Test
  fun `test attach jdbc console for framework tasks`() {
    val course = sqlCourse {
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

  @Test
  fun `test database view structure`() {
    @Suppress("SqlDialectInspection")
    val course = sqlCourse {
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
        eduTask("task1") {
          taskFile("src/task.sql")
        }
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

    val tree = prepareDatabaseView()

    checkDatabaseTree(tree, """
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
         $EMPTY_DATA_SOURCE_PLACEHOLDER
       -Group (lesson2) inside Root Group
        -task1 (1): DSN
         $EMPTY_DATA_SOURCE_PLACEHOLDER
        -task3: DSN
         $EMPTY_DATA_SOURCE_PLACEHOLDER
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
          $EMPTY_DATA_SOURCE_PLACEHOLDER
         -task6: DSN
          $EMPTY_DATA_SOURCE_PLACEHOLDER
        -Group (section1/lesson4) inside Group (section1) inside Root Group
         -task7: DSN
          $EMPTY_DATA_SOURCE_PLACEHOLDER
         -task8: DSN
          $EMPTY_DATA_SOURCE_PLACEHOLDER
       -Group (section 2\) inside Root Group
        -Group (section 2\/les s on5) inside Group (section 2\) inside Root Group
         -/task:1:0/: DSN
          $EMPTY_DATA_SOURCE_PLACEHOLDER
         -task9: DSN
          $EMPTY_DATA_SOURCE_PLACEHOLDER
    """)
  }

  @Suppress("SqlDialectInspection", "SqlNoDataSourceInspection")
  @Test
  fun `test database initialization`() {
    val course = sqlCourse {
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
  @Test
  fun `test database initialization in framework lessons`() {
    val course = sqlCourse {
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
}
