package com.jetbrains.edu.sql.kotlin.update

import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.sql.psi.SqlLanguage
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleCourseBuilderBase.Companion.INIT_SQL
import com.jetbrains.edu.sql.kotlin.SqlCourseGenerationTestBase
import java.util.*

class SqlCourseUpdateTest : SqlCourseGenerationTestBase() {

  @Suppress("SqlDialectInspection")
  fun `test data sources after course update`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 11) {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """create table if not exists STUDENTS_1;""")

        }
        eduTask("task2", stepId = 12) {
          taskFile("src/task.sql")
        }
        eduTask("task3", stepId = 13) {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """create table if not exists STUDENTS_3;""")
        }
      }
      lesson("lesson2", id = 2) {
        eduTask("task4", stepId = 14) {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """create table if not exists STUDENTS_4;""")
        }
        eduTask("task5", stepId = 15) {
          taskFile("src/task.sql")
        }
      }
    } as EduCourse

    createCourseStructure(course)

    val courseFromServer = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 11) {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """create table if not exists STUDENTS_1;""")
        }
        eduTask("task2", stepId = 12) {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """create table if not exists STUDENTS_2;""")
        }
      }
      lesson("lesson3", id = 3) {
        eduTask("task6", stepId = 16) {
          taskFile("src/task.sql")

        }
        eduTask("task7", stepId = 17) {
          taskFile("src/task.sql")
          sqlTaskFile(INIT_SQL, """create table if not exists STUDENTS_7;""")
        }
      }
    } as EduCourse

    setTopLevelSection(courseFromServer)

    MarketplaceCourseUpdater(project, course, 2).updateCourseWithRemote(courseFromServer)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    waitWhileDataSourceSyncInProgress()

    checkAllTasksHaveDataSource(course)
    assertEquals(course.allTasks.size, LocalDataSourceManager.getInstance (project).dataSources.size)

    checkTable(course.findTask("lesson1", "task1"), "STUDENTS_1", shouldExist = true)
    // Don't run `init.sql` script since task2 is not new
    checkTable(course.findTask("lesson1", "task2"), "STUDENTS_2", shouldExist = false)
    checkTable(course.findTask("lesson3", "task7"), "STUDENTS_7", shouldExist = true)
  }

  override fun createCourseStructure(course: Course) {
    super.createCourseStructure(course)
    if (course is EduCourse) {
      setTopLevelSection(course)
    }
  }

  private fun setTopLevelSection(course: EduCourse) {
    if (course.lessons.isNotEmpty()) {
      // it's a hack.Originally we need to put here and id of remote section for top-level lesson
      course.sectionIds = Collections.singletonList(1)
    }
  }
}
