package com.jetbrains.edu.cpp

import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.fileTree

class CppCourseBuilderTest : CourseGenerationTestBase<CppProjectSettings>() {

  override val courseBuilder = CppCourseBuilder()
  override val defaultSettings = CppProjectSettings()

  fun `test study course structure`() {
    val course = course(language = OCLanguage.getInstance()) {
      lesson {
        eduTask {
          taskFile("task.cpp")
          taskFile("CMakeLists.txt")
        }
      }
      additionalFiles {
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        file("task.cpp")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }
}
