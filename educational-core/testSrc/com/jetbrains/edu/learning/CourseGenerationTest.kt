package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.learning.configuration.PlainTextCourseBuilder
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat

class CourseGenerationTest : CourseGenerationTestBase<Unit>() {
  override val courseBuilder: EduCourseBuilder<Unit> = PlainTextCourseBuilder()
  override val defaultSettings: Unit = Unit

  fun `test do not open invisible files after course creation`() {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("invisible.txt", visible = false)
          taskFile("visible.txt")
        }
      }
    }
    createCourseStructure(course)

    val invisible = findFile("lesson1/task1/invisible.txt")
    val visible = findFile("lesson1/task1/visible.txt")
    val openFiles = FileEditorManager.getInstance(project).openFiles.toList()
    assertThat(openFiles, not(hasItem(invisible)))
    assertThat(openFiles, hasItem(visible))
  }
}
