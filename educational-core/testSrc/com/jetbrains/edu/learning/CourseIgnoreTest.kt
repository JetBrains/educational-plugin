package com.jetbrains.edu.learning

import com.jetbrains.edu.coursecreator.AdditionalFilesUtils


class CourseIgnoreTest : EduTestCase() {

  fun `test first level ignored`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      additionalFiles {
        eduFile(".courseignore", "ignored.txt")
        eduFile("ignored.txt")
        eduFile("not-ignored.txt")
      }
    }
    val additionalFiles = AdditionalFilesUtils.collectAdditionalFiles(course, project)
    assertSameElements(additionalFiles.map { it.name }, listOf("not-ignored.txt"))
  }

  fun `test second level ignored`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
          taskFile("NotIgnored.txt")
        }
      }
      additionalFiles {
        eduFile(".courseignore",
                """
                  |tmp/ignored.txt
                  |lesson1/task1/NotIgnored.txt
                """.trimMargin())
        eduFile("not-ignored.txt")
        eduFile("tmp/ignored.txt")
        eduFile("tmp/not-ignored.txt")
      }
    }
    val additionalFiles = AdditionalFilesUtils.collectAdditionalFiles(course, project)
    assertSameElements(additionalFiles.map { it.name }, listOf("not-ignored.txt", "tmp/not-ignored.txt"))

    val task = course.findTask("lesson1", "task1")
    assertNotNull(task)
    assertSameElements(task.taskFiles.keys, listOf("NotIgnored.txt", "Task.kt"))
  }
}