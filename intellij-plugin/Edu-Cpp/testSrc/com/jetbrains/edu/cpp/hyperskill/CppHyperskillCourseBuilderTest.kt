package com.jetbrains.edu.cpp.hyperskill

import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import org.junit.Test

class CppHyperskillCourseBuilderTest  : CourseGenerationTestBase<CppProjectSettings>() {
  override val defaultSettings = CppProjectSettings()

  @Test
  fun `test create new cc edu Hyperskill course`() {
    val course = course(
      language = OCLanguage.getInstance(),
      courseMode = CourseMode.EDUCATOR,
      courseProducer = ::HyperskillCourse
    ) { }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("task.md")
        file("CMakeLists.txt")
      }
      dir("cmake") {
        file("utils.cmake")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }
}