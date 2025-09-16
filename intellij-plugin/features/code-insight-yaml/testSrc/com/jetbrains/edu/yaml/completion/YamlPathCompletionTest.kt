package com.jetbrains.edu.yaml.completion

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.writeText
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class YamlPathCompletionTest : YamlCompletionTestBase() {

  @Test
  fun `test task file completion`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir)!!
    GeneratorUtils.createTextChildFile(project, taskDir, "src/foo.txt", "")

    doSingleCompletion(task, """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: src/foo<caret>
      |  visible: true      
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: src/foo.txt
      |  visible: true      
    """.trimMargin("|"))
  }

  @Test
  fun `test task file completion 2`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
          taskFile("src/foo.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")

    doSingleCompletion(task, """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: src/foo<caret>
      |  visible: true      
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: src/foo.txt
      |  visible: true      
    """.trimMargin("|"))
  }

  @Test
  fun `test do not suggest existing paths while completion`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir)!!
    GeneratorUtils.createTextChildFile(project, taskDir, "src/taskfile2.txt", "")

    doSingleCompletion(task, """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: src/task<caret>
      |  visible: true      
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: src/taskfile2.txt
      |  visible: true      
    """.trimMargin("|"))
  }

  @Test
  fun `test do not suggest excluded from archive files while completion 1`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir)!!
    GeneratorUtils.createTextChildFile(project, taskDir, "taskfile2.txt", "")

    doSingleCompletion(task, """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: task<caret>
      |  visible: true      
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: taskfile2.txt
      |  visible: true      
    """.trimMargin("|"))
  }

  @Test
  fun `test do not suggest excluded from archive files while completion 2`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir)!!
    GeneratorUtils.createTextChildFile(project, taskDir, "some_module.iml", "")

    checkNoCompletion(task, """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: .hidden<caret>
      |  visible: false      
    """.trimMargin("|"))
  }

  @Test
  fun `test task file extend completion`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir)!!
    GeneratorUtils.createTextChildFile(project, taskDir, "src/task.txt", "")

    doSingleCompletion(task, """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: src/tas<caret>
      |  visible: true      
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/taskfile1.txt
      |  visible: true
      |- name: src/task.txt
      |  visible: true      
    """.trimMargin("|"), invocationCount = 2)
  }

  @Test
  fun `test directories completion in task config`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir)!!
    GeneratorUtils.createTextChildFile(project, taskDir, "src/task.txt", "")

    doSingleCompletion(task, """
      |type: edu
      |files:
      |- name: sr<caret>
      |  visible: true      
    """.trimMargin("|"), """
      |type: edu
      |files:
      |- name: src/
      |  visible: true      
    """.trimMargin("|"))
  }

  @Test
  fun `test course content completion`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {}
    }

    runWriteAction {
      project.courseDir.createChildDirectory(this, "section2")
    }

    doSingleCompletion(course, """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |- sec<caret>
    """.trimMargin("|"), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |- section2
    """.trimMargin("|"))
  }

  @Test
  fun `test lesson content completion`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val lesson = course.getLesson("lesson1")!!
    val lessonDir = lesson.getDir(project.courseDir)!!
    runWriteAction { lessonDir.createChildDirectory(this, "task2") }

    doSingleCompletion(lesson, """
      |content:
      |- task1
      |- tas<caret>
    """.trimMargin("|"), """
      |content:
      |- task1
      |- task2
    """.trimMargin("|"))
  }

  @Test
  fun `test section content completion`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section("section1") {
        lesson("lesson1") {}
        lesson("lesson2") {}
      }
    }

    val section = course.getSection("section1")!!

    doSingleCompletion(section, """
      |content:
      |- lesson1
      |- less<caret>
    """.trimMargin("|"), """
      |content:
      |- lesson1
      |- lesson2
    """.trimMargin("|"))
  }

  @Test
  fun `test lesson content extend completion`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val lesson = course.getLesson("lesson1")!!
    val lessonDir = lesson.getDir(project.courseDir)!!
    runWriteAction { lessonDir.createChildDirectory(this, "task2") }

    doSingleCompletion(lesson, """
      |content:
      |- task1
      |- tas<caret>
    """.trimMargin("|"), """
      |content:
      |- task1
      |- task2
    """.trimMargin("|"), invocationCount = 2)
  }

  @Test
  fun `complete additional file`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {}
    nonAdditionalFile("a.txt")
    nonAdditionalFile("b.txt")
    nonAdditionalFile("dir/c.txt")
    checkMultipleCompletion(
      course, """
      type: edu
      additional_files:
        - name: <caret>
    """.trimIndent(),
      "a.txt", "b.txt", "dir/", "lesson1/"
    )
  }

  @Test
  fun `complete additional file and do not suggest existing`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      additionalFile("b.txt")
    }
    nonAdditionalFile("a.txt")
    nonAdditionalFile("dir/c.txt")
    checkMultipleCompletion(
      course, """
      type: edu
      additional_files:
        - name: b.txt
        - name: <caret>
    """.trimIndent(),
      "a.txt", "dir/", "lesson1/"
    )
  }

  @Test
  fun `complete additional file and do not suggest MUST_EXCLUDE files`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      additionalFile("b.txt")
    }
    nonAdditionalFile("a.txt")
    nonAdditionalFile("module.iml")
    nonAdditionalFile(".coursecreator/course.zip")
    nonAdditionalFile("dir/course.zip")
    checkMultipleCompletion(
      course, """
      type: edu
      additional_files:
        - name: b.txt
        - name: <caret>
    """.trimIndent(),
      "a.txt", "lesson1/", "dir/"
    )
  }

  private fun nonAdditionalFile(path: String, contents: String = "") {
    runWriteAction {
      project.courseDir.findOrCreateFile(path).writeText(contents)
    }
  }
}
