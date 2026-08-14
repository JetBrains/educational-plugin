package com.jetbrains.edu.learning.newproject

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase

abstract class CourseMetadataProcessorTestBase : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  protected fun createCourseWithMetadata(metadata: Map<String, String>): Course {
    val course = course {
      section("section1", id = 1) {
        lesson("lesson1", id = 11) {
          eduTask("task1", stepId = 111) {
            taskFile("foo.txt")
          }
          eduTask("task2", stepId = 112) {
            taskFile("foo.txt")
          }
        }
        lesson("lesson2", id = 12) {
          eduTask("task3", stepId = 121) {
            taskFile("foo.txt")
          }
        }
      }
    }

    createCourseStructure(course, metadata)
    return course
  }
}
