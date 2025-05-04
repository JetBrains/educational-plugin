package com.jetbrains.edu.yaml.inspections

import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class TaskFileNotFoundInspectionTest : YamlInspectionsTestBase(TaskFileNotFoundInspection::class) {

  @Test
  fun `test create task file`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val task = course.findTask("lesson1", "task1")

    val expectedTaskFiles = listOf("src/taskfile1.txt" to true)
    doTest(task, "Create file", """
      |type: edu
      |files:
      |- name: <error descr="Cannot find `src/taskfile1.txt` file">src/taskfile1.txt<caret></error>
      |  visible: true
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
    """.trimMargin("|"), expectedTaskFiles)
  }

  @Test
  fun `test create task file if there are several non-existing files`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/existing_file_1.txt")
          taskFile("src/existing_file_2.txt")
        }
      }
    }

    val task = course.findTask("lesson1", "task1")

    val text1 = """
      |type: edu
      |files:
      |- name: src/existing_file_1.txt
      |  visible: true
      |- name: <error descr="Cannot find `src/non_existing_file_1.txt` file">src/non_existing_file_1.txt</error>
      |  visible: true
      |- name: <error descr="Cannot find `src/non_existing_file_2.txt` file">src/non_existing_file_2.txt<caret></error>
      |  visible: true
      |- name: src/existing_file_2.txt
      |  visible: true
    """.trimMargin("|")

    val text2 = """
      |type: edu
      |files:
      |- name: src/existing_file_1.txt
      |  visible: true
      |- name: <error descr="Cannot find `src/non_existing_file_1.txt` file">src/non_existing_file_1.txt<caret></error>
      |  visible: true
      |- name: src/non_existing_file_2.txt
      |  visible: true
      |- name: src/existing_file_2.txt
      |  visible: true
    """.trimMargin("|")

    val text3 = """
      |type: edu
      |files:
      |- name: src/existing_file_1.txt
      |  visible: true
      |- name: src/non_existing_file_1.txt
      |  visible: true
      |- name: src/non_existing_file_2.txt
      |  visible: true
      |- name: src/existing_file_2.txt
      |  visible: true
    """.trimMargin("|")

    doTest(task, "Create file", text1, text2, listOf(
        "src/existing_file_1.txt" to true,
        "src/existing_file_2.txt" to true
    ))
    doTest(task, "Create file", text2, text3, listOf(
      "src/existing_file_1.txt" to true,
      "src/non_existing_file_1.txt" to true,
      "src/non_existing_file_2.txt" to true,
      "src/existing_file_2.txt" to true
    ))
  }

  @Test
  fun `test create invisible task file`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val task = course.findTask("lesson1", "task1")

    val expectedTaskFiles = listOf("src/taskfile1.txt" to false)
    doTest(task, "Create file", """
      |type: edu
      |files:
      |- name: <error descr="Cannot find `src/taskfile1.txt` file">src/taskfile1.txt<caret></error>
      |  visible: false
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: false
    """.trimMargin("|"), expectedTaskFiles)
  }

  @Test
  fun `test keep position`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }

    val task = course.findTask("lesson1", "task1")

    val expectedTaskFiles = listOf("src/taskfile0.txt" to true, "src/taskfile1.txt" to true)
    doTest(task, "Create file", """
      |type: edu
      |files:
      |- name: <error descr="Cannot find `src/taskfile0.txt` file">src/taskfile0.txt<caret></error>
      |  visible: true
      |- name: src/taskfile1.txt
      |  visible: true
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile0.txt
      |  visible: true
      |- name: src/taskfile1.txt
      |  visible: true
    """.trimMargin("|"), expectedTaskFiles)
  }

  @Test
  fun `test do not provide quick fix for invalid paths 1`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    testQuickFixIsUnavailable(task, "Create file", """
      |type: edu
      |files:
      |- name: <error descr="File path `src\taskfile1.txt` is invalid">src\taskfile1.txt</error>
      |  visible: true
    """.trimMargin("|"))
  }

  @Test
  fun `test do not provide quick fix for invalid paths 2`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    testQuickFixIsUnavailable(task, "Create file", """
      |type: edu
      |files:
      |- name: <error descr="File path `src//taskfile1.txt` is invalid">src//taskfile1.txt</error>
      |  visible: true
    """.trimMargin("|"))
  }

  private fun doTest(task: Task, quickFixName: String, before: String, after: String, expectedTaskFiles: List<Pair<String, Boolean>>) {
    testQuickFix(task, quickFixName, before, after)
    val actualTaskFiles = task.taskFiles.values.map { it.name to it.isVisible }
    assertEquals(expectedTaskFiles, actualTaskFiles)
    val taskDir = task.getDir(project.courseDir)!!
    for ((path, _) in expectedTaskFiles) {
      assertNotNull("Failed to find `$path` file", taskDir.findFileByRelativePath(path))
    }
  }
}
