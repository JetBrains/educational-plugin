package com.jetbrains.edu.learning.newproject

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase

abstract class CourseParamsProcessorTestBase : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  protected fun createCourseWithMetadata(metadata: Map<String, String>) {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("foo.txt")
        }
      }
    }

    createCourseStructure(course, metadata)
  }
}
