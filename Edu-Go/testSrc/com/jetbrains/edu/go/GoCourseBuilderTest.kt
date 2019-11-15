package com.jetbrains.edu.go

import com.goide.GoLanguage
import com.goide.sdk.GoSdk
import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse

class GoCourseBuilderTest : CourseGenerationTestBase<GoProjectSettings>() {
  override val defaultSettings: GoProjectSettings = GoProjectSettings(GoSdk.NULL)

  fun `test new educator course`() {
    val newCourse = newCourse(GoLanguage.INSTANCE)
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("main") {
          file("main.go")
        }
        dir("test") {
          file("task_test.go")
        }
        file("task.go")
        file("task.html")
      }
      file("go.mod")
    }.assertEquals(rootDir)
  }

  fun `test study course structure`() {
    val course = course(language = GoLanguage.INSTANCE) {
      lesson {
        eduTask {
          taskFile("main/main.go")
          taskFile("test/task_test.go")
          taskFile("task.go")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("main") {
          file("main.go")
        }
        dir("test") {
          file("task_test.go")
        }
        file("task.go")
        file("task.html")
      }
      file("go.mod")
    }.assertEquals(rootDir)
  }
}
