package com.jetbrains.edu.yaml

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
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

  private fun doSingleCompletion(item: StudyItem, before: String, after: String) {
    openConfigFileWithText(item, before)
    val variants = myFixture.completeBasic()
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