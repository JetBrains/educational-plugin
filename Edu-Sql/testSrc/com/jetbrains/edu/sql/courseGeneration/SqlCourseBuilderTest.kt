package com.jetbrains.edu.sql.courseGeneration

import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse

class SqlCourseBuilderTest : CourseGenerationTestBase<Unit>() {
  override val defaultSettings: Unit get() = Unit

  fun `test new educator course`() {
    val newCourse = newCourse(SqlLanguage.INSTANCE)
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        file("task.sql")
        file("task.md")
      }
    }.assertEquals(rootDir)
  }

  fun `test create existent educator course`() {
    val course = course(language = SqlLanguage.INSTANCE, courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("task.sql")
          taskFile("migration.sql")
          taskFile("data/data.sql")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        file("task.sql")
        file("migration.sql")
        dir("data") {
          file("data.sql")
        }
        file("task.md")
      }
    }.assertEquals(rootDir)
  }


  fun `test study course structure`() {
    val course = course(language = SqlLanguage.INSTANCE) {
      lesson {
        eduTask {
          taskFile("task.sql")
          taskFile("migration.sql")
          taskFile("data/data.sql")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        file("task.sql")
        file("migration.sql")
        dir("data") {
          file("data.sql")
        }
        file("task.md")
      }
    }.assertEquals(rootDir)
  }
}
