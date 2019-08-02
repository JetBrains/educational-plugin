package com.jetbrains.edu.yaml

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.yaml.skipYamlCompletionTests
import kotlin.test.assertNotEquals

class YamlCompletionTest : YamlCodeInsightTest() {

  // can't use existing shouldRunTest method because ApplicationInfo isn't initialized when it's called
  // and we can't check ide type
  override fun runTest() {
    // BACKCOMPAT: 2019.1 tests fail in Studio 191 and IJ 183
    @Suppress("ConstantConditionIf")
    if (!skipYamlCompletionTests) {
      super.runTest()
    }
  }

  fun `test completion for course programming language`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: <caret>Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertEquals(2, lookupElements.size)
    assertContainsElements(lookupElements.map { it.lookupString }, FakeGradleBasedLanguage.displayName,
                           PlainTextLanguage.INSTANCE.displayName)
  }

  fun `test completion for course programming language version`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: <caret>1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertEquals(1, lookupElements.size)
    assertContainsElements(lookupElements.map { it.lookupString }, "1.42")
  }

  fun `test completion for course human language`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: <caret>Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertNotEquals(0, lookupElements.size)
    assertContainsElements(lookupElements.map { it.lookupString }, "English", "Russian", "German")
  }

  fun `test completion for course environment`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: <caret>Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertEquals(2, lookupElements.size)
    assertContainsElements(lookupElements.map { it.lookupString }, "Android")
  }

  fun `test no completion in non-config file`() {
    myFixture.configureByText("random.yaml", """
      |title: Test Course
      |type: coursera
      |language: <caret>Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    val lookupElements = myFixture.completeBasic()
    assertEmpty(lookupElements)
  }

  fun `test task file completion`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getTaskDir(project)!!
    GeneratorUtils.createChildFile(taskDir, "src/foo.txt", "")

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

  fun `test task file completion 2`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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

  fun `test do not suggest existing paths while completion`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getTaskDir(project)!!
    GeneratorUtils.createChildFile(taskDir, "src/taskfile2.txt", "")

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

  fun `test do not suggest excluded from archive files while completion`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getTaskDir(project)!!
    GeneratorUtils.createChildFile(taskDir, "taskfile2.txt", "")

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

  fun `test task file extend completion`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/taskfile1.txt")
        }
      }
    }
    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getTaskDir(project)!!
    GeneratorUtils.createChildFile(taskDir, "src/task.txt", "")

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

  fun `test course content completion`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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

  fun `test lesson content completion`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val lesson = course.getLesson("lesson1")!!
    val lessonDir = lesson.getLessonDir(project)!!
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

  fun `test section content completion`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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

  fun `test lesson content extend completion`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val lesson = course.getLesson("lesson1")!!
    val lessonDir = lesson.getLessonDir(project)!!
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

  private fun doSingleCompletion(item: StudyItem, before: String, after: String, invocationCount: Int = 1) {
    openConfigFileWithText(item, before)
    val variants = myFixture.complete(CompletionType.BASIC, invocationCount)
    if (variants != null) {
      if (variants.size == 1) {
        myFixture.type('\n')
        return
      }
      error("Expected a single completion, but got ${variants.size}\n" + variants.joinToString("\n") { it.lookupString })
    }
    myFixture.checkResult(after)
  }
}