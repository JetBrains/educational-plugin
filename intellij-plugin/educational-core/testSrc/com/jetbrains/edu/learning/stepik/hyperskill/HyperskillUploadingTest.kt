package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import org.junit.Test

class HyperskillUploadingTest : EduTestCase() {
  @Test
  fun `test collect solution files`() {
    val course = createHyperskillCourse()
    val task = course.findTask("lesson1", "task1")
    val files = getSolutionFiles(project, task)
    assertEquals(3, files.size)
    for (file in files) {
      assertEquals(mapOf("src/Task.kt" to true, "src/Baz.kt" to false, "test/Tests1.kt" to false)[file.name], file.isVisible)
    }
  }

  private fun createHyperskillCourse(): HyperskillCourse {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}", visible = true)
          taskFile("src/Baz.kt", "fun baz() {}", visible = false)
          taskFile("test/Tests1.kt", "fun tests1() {}", visible = false)
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))
    return course
  }
}
