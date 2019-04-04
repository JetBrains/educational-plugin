package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.fileTree
import junit.framework.TestCase
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.hasItem
import org.junit.Assert.assertThat

class CCCreateTaskTest : EduActionTestCase() {

  fun `test create task in lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val lessonFile = findFile("lesson1")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task2")) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }
    TestCase.assertEquals(2, course.lessons[0].taskList.size)
  }

  fun `test create task in lesson in section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    val lessonFile = findFile("section1/lesson1")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task2")) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }
    TestCase.assertEquals(2, course.sections[0].lessons[0].taskList.size)
  }

  fun `test create task in empty lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
    }
    val lessonFile = findFile("lesson1")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task1")) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }
    TestCase.assertEquals(1, course.lessons[0].taskList.size)
  }

  fun `test create task after task`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val taskFile = findFile("lesson1/task1")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task01", 2)) {
      testAction(dataContext(taskFile), CCCreateTask())
    }

    val lesson = course.lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task before task`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val taskFile = findFile("lesson1/task2")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task01", 2)) {
      testAction(dataContext(taskFile), CCCreateTask())
    }

    val lesson = course.lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task before task with custom name`() {
    val customTaskName = "Custom Task Name"
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask(customTaskName) {
          taskFile("taskFile1.txt")
        }
      }
    }
    val taskFile = findFile("lesson1/$customTaskName")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task01", 2)) {
      testAction(dataContext(taskFile), CCCreateTask())
    }

    val lesson = course.lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask(customTaskName)!!.index)
  }

  fun `test create task after task in section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    val taskFile = findFile("section1/lesson1/task1")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task01", 2)) {
      testAction(dataContext(taskFile), CCCreateTask())
    }

    val lesson = course.sections[0].lessons[0]
    TestCase.assertEquals(3, lesson.taskList.size)
    TestCase.assertEquals(1, lesson.getTask("task1")!!.index)
    TestCase.assertEquals(2, lesson.getTask("task01")!!.index)
    TestCase.assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task not available on course`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val sourceVFile = LightPlatformTestCase.getSourceRoot()
    val action = CCCreateTask()
    val event = TestActionEvent(dataContext(sourceVFile!!), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }

  fun `test create task not available on section`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    val sourceVFile = findFile("section1")
    val action = CCCreateTask()
    val event = TestActionEvent(dataContext(sourceVFile), action)
    action.beforeActionPerformedUpdate(event)
    TestCase.assertFalse(event.presentation.isEnabledAndVisible)
  }

  fun `test create framework task without test copy`() = doCreateFrameworkTaskTest(false)
  fun `test create framework task with test copy`() = doCreateFrameworkTaskTest(true)

  private fun doCreateFrameworkTaskTest(copyTests: Boolean) {
    val lessonName = "lesson1"
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage) {
      frameworkLesson(lessonName) {
        eduTask("task1") {
          taskFile("src/Task.kt")
          taskFile("test/Tests1.kt", "//text changed", visible = false)
        }
      }
    }

    val lessonFile = findFile(lessonName)
    val newTaskName = "task2"
    withMockCreateStudyItemUi(MockNewFrameworkTaskUi(newTaskName, copyTests = copyTests)) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }

    val newTask = course.findTask(lessonName, newTaskName)
    val copyTestFileMatcher = hasItem("test/Tests1.kt")
    val notCopyTestFileMatcher = hasItem("test/Tests.kt")
    val testFileMatcher = if (copyTests) copyTestFileMatcher else notCopyTestFileMatcher

    assertThat(newTask.taskFiles.keys, allOf(hasItem("src/Task.kt"), testFileMatcher))
    val testText = if (copyTests) "//text changed" else getDefaultTestText(course)
    TestCase.assertEquals(testText, newTask.taskFiles[if (copyTests) "test/Tests1.kt" else "test/Tests.kt"]?.text)

    fileTree {
      dir(lessonName) {
        dir("task1") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests1.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
          }
          if (copyTests) {
            dir("test") {
              file("Tests1.kt")
            }
          }
          else {
            dir("test") {
              file("Tests.kt")
            }
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  private fun getDefaultTestText(course: Course): String? {
    val testTemplateName = course.configurator?.courseBuilder?.testTemplateName ?: return null
    return GeneratorUtils.getInternalTemplateText(testTemplateName)
  }
}

private class MockNewFrameworkTaskUi(name: String, index: Int? = null, private val copyTests: Boolean = false) : MockNewStudyItemUi(name, index) {
  override fun showDialog(project: Project, model: NewStudyItemUiModel, additionalPanels: List<AdditionalPanel>): NewStudyItemInfo? {
    return super.showDialog(project, model, additionalPanels)?.apply {
      putUserData(CCCreateTask.COPY_TESTS_FROM_PREV_TASK, copyTests)
    }
  }
}