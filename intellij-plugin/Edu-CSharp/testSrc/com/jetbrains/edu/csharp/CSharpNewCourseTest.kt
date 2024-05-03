package com.jetbrains.edu.csharp

import com.intellij.codeInsight.CodeInsightSettings
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage
class CSharpNewCourseTest : CourseGenerationTestBase<CSharpProjectSettings>() {
  override val defaultSettings: CSharpProjectSettings = CSharpProjectSettings()
  private lateinit var state: CodeInsightSettings

  override fun setUp() {
    super.setUp()
    state = CodeInsightSettings()
  }

  override fun tearDown() {
    try {
      state.state?.let { CodeInsightSettings.getInstance().loadState(it) }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  fun `test new educator course`() {
    val course = csharpCourse(CourseMode.EDUCATOR)
    createCourseStructure(course)
    val expectedFileTree = fileTree {
      file("${course.name}.csproj")
      dir("lesson1") {
        dir("task1") {
          file("task.cs")
          file("task.md")
          file("test.cs")
        }
      }
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test student course`() {
    val course = csharpCourse(CourseMode.STUDENT) {
      additionalFile("${course.name}.csproj")
      lesson("Lesson 1") {
        eduTask("Task 1") {
          taskFile("task.cs")
          taskFile("test.cs")
        }
        eduTask("Task 2") {
          taskFile("task.cs")
          taskFile("test.cs")
        }
        eduTask("Task 3") {
          taskFile("task.cs")
          taskFile("test.cs")
        }
      }
      lesson("Lesson 2") {
        eduTask("Task 1") {
          taskFile("task.cs")
          taskFile("test.cs")
        }
        eduTask("Task 2") {
          taskFile("task.cs")
          taskFile("test.cs")
        }
      }
    }
    createCourseStructure(course)
    val expectedFileTree = fileTree {
      file("${course.name}.csproj")
      dir("Lesson 1") {
        dir("Task 1") {
          file("task.cs")
          file("task.md")
          file("test.cs")
        }
        dir("Task 2") {
          file("task.cs")
          file("task.md")
          file("test.cs")
        }
        dir("Task 3") {
          file("task.cs")
          file("task.md")
          file("test.cs")
        }
      }
      dir("Lesson 2") {
        dir("Task 1") {
          file("task.cs")
          file("task.md")
          file("test.cs")
        }
        dir("Task 2") {
          file("task.cs")
          file("task.md")
          file("test.cs")
        }
      }
    }
    expectedFileTree.assertEquals(rootDir)
  }

  private fun csharpCourse(courseMode: CourseMode, buildCourse: CourseBuilder.() -> Unit = {}): Course = course(
    language = CSharpLanguage,
    courseMode = courseMode,
    buildCourse = buildCourse
  )
}