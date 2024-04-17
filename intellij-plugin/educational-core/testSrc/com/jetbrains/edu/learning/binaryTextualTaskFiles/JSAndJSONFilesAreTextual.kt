package com.jetbrains.edu.learning.binaryTextualTaskFiles

import com.jetbrains.edu.learning.CourseReopeningTestBase
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test

class JSAndJSONFilesAreTextual : CourseReopeningTestBase<EmptyProjectSettings>() {

  override val defaultSettings = EmptyProjectSettings

  @Test
  fun `test json and js files are preserved in the test folder`() {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("hello.txt", InMemoryTextualContents("hello.txt"))
          taskFile("hello.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
          dir("test") {
            // JS, JSON files have unknown types in tests, but we simulate that IDE knows them by writing InMemoryTextualContents.
            // We expect, that after reopening the project, they will again be textual
            taskFile("testData.json", InMemoryTextualContents("{}"))
            taskFile("test.js", InMemoryTextualContents("var x"))
          }
        }
      }
    }

    openStudentProjectThenReopenStudentProject(course) { project ->
      val projectCourse = project.course!!
      val task1 = projectCourse.findTask("lesson1", "task1")

      val json = task1.taskFiles["test/testData.json"]!!
      assertContentsEqual("test/testData.json", InMemoryTextualContents("{}"), json.contents)

      val js = task1.taskFiles["test/test.js"]!!
      assertContentsEqual("test/test.js", InMemoryTextualContents("var x"), js.contents)
    }
  }
}