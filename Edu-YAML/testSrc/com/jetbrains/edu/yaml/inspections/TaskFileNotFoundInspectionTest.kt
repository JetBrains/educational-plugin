package com.jetbrains.edu.yaml.inspections

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class TaskFileNotFoundInspectionTest : YamlInspectionsTestBase(TaskFileNotFoundInspection::class) {

  override fun runTest() {
    // tests fail in AS because of Feature Usage Statistics exception
    if (!EduUtils.isAndroidStudio()) {
      super.runTest()
    }
  }

  fun `test create task file`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val task = course.findTask("lesson1", "task1")

    val expectedTaskFiles = listOf("src/taskfile1.txt" to true)
    doTest(task, "Create file", """
      |type: edu
      |files:
      |- name: <error descr="Cannot find 'src/taskfile1.txt' file">src/taskfile1.txt<caret></error>
      |  visible: true
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
    """.trimMargin("|"), expectedTaskFiles)
  }

  fun `test create invisible task file`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val task = course.findTask("lesson1", "task1")

    val expectedTaskFiles = listOf("src/taskfile1.txt" to false)
    doTest(task, "Create file", """
      |type: edu
      |files:
      |- name: <error descr="Cannot find 'src/taskfile1.txt' file">src/taskfile1.txt<caret></error>
      |  visible: false
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: false
    """.trimMargin("|"), expectedTaskFiles)
  }

  fun `test keep position`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
      |- name: <error descr="Cannot find 'src/taskfile0.txt' file">src/taskfile0.txt<caret></error>
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

  fun `test do not provide quick fix for invalid paths 1`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
      |- name: <error descr="Cannot find 'src\taskfile1.txt' file">src\taskfile1.txt</error>
      |  visible: true
    """.trimMargin("|"))
  }

  fun `test do not provide quick fix for invalid paths 2`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
      |- name: <error descr="Cannot find 'src//taskfile1.txt' file">src//taskfile1.txt</error>
      |  visible: true
    """.trimMargin("|"))
  }

  private fun doTest(task: Task, quickFixName: String, before: String, after: String, expectedTaskFiles: List<Pair<String, Boolean>>) {
    testQuickFix(task, quickFixName, before, after)
    val actualTaskFiles = task.taskFiles.values.map { it.name to it.isVisible }
    assertEquals(expectedTaskFiles, actualTaskFiles)
    val taskDir = task.getTaskDir(project)!!
    for ((path, _) in expectedTaskFiles) {
      assertNotNull("Failed to find `$path` file", taskDir.findFileByRelativePath(path))
    }
  }
}
