package com.jetbrains.edu.sql.jvm.gradle.courseGeneration

import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleStartupActivity
import com.jetbrains.edu.sql.jvm.gradle.SqlTestLanguage
import com.jetbrains.edu.sql.jvm.gradle.sqlCourse
import com.jetbrains.edu.sql.jvm.gradle.sqlTestLanguage
import org.junit.Test

class SqlGradleCourseBuilderTest : JvmCourseGenerationTestBase() {

  override fun setUp() {
    super.setUp()
    SqlGradleStartupActivity.disable(testRootDisposable)
  }

  @Test
  fun `test new educator course with java tests`() {
    val newCourse = newCourse(SqlLanguage.INSTANCE)
    newCourse.sqlTestLanguage = SqlTestLanguage.JAVA
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
        }
        dir("test") {
          file("SqlTest.java")
        }
        file("init.sql")
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)

    assertListOfAdditionalFiles(newCourse,
      "build.gradle" to null,
      "settings.gradle" to null
    )
  }

  @Test
  fun `test new educator course with kotlin tests`() {
    val newCourse = newCourse(SqlLanguage.INSTANCE)
    newCourse.sqlTestLanguage = SqlTestLanguage.KOTLIN
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
        }
        dir("test") {
          file("SqlTest.kt")
        }
        file("init.sql")
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  @Test
  fun `test create existent educator course`() {
    val course = sqlCourse(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/SqlTest.kt")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
          file("migration.sql")
          dir("data") {
            file("data.sql")
          }
        }
        dir("test") {
          file("SqlTest.kt")
        }
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  @Test
  fun `test study course structure`() {
    val course = sqlCourse {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/SqlTest.kt")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
          file("migration.sql")
          dir("data") {
            file("data.sql")
          }
        }
        dir("test") {
          file("SqlTest.kt")
        }
        file("task.md")
      }
      dir("gradle") {
        dir("wrapper") {
          file("gradle-wrapper.properties")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  @Test
  fun `test student course additional files with kts files`() {
    val newCourse = course(language = SqlLanguage.INSTANCE) {
      additionalFile("build.gradle.kts", "")
      additionalFile("settings.gradle.kts", "")
    }
    createCourseStructure(newCourse)

    assertListOfAdditionalFiles(newCourse,
      // TODO(uncomment following line after either EDU-8545 or EDU-8540 will be merged)
      //"gradle/wrapper/gradle-wrapper.properties" to null,
      "build.gradle.kts" to null,
      "settings.gradle.kts" to null
      //build.gradle and settings.gradle are not created
    )
  }
}
