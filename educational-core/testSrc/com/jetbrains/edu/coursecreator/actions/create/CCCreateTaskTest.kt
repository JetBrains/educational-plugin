package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestVFiles
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.yaml.YamlDeepLoader
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.hasItem
import org.junit.Assert.assertThat

class CCCreateTaskTest : EduActionTestCase() {

  fun `test create task in lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, createYamlConfigs=true) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val lessonFile = findFile("lesson1")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task2")) {
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
    }
    assertEquals(2, course.lessons[0].taskList.size)

    UIUtil.dispatchAllInvocationEvents()
    val loadedCourseFromYaml = YamlDeepLoader.loadCourse(project)
    assertNotNull(loadedCourseFromYaml)
    assertEquals(course.lessons[0].taskList.size, loadedCourseFromYaml!!.lessons[0].taskList.size)

    val task = course.lessons[0].taskList[1]
    checkOpenedFiles(task)
  }

  fun `test create task in lesson in section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
    }
    assertEquals(2, course.sections[0].lessons[0].taskList.size)
  }

  fun `test create task in empty lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
    }
    val lessonFile = findFile("lesson1")

    withMockCreateStudyItemUi(MockNewStudyItemUi("task1")) {
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
    }
    assertEquals(1, course.lessons[0].taskList.size)
  }

  fun `test create task after task`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
      testAction(CCCreateTask.ACTION_ID, dataContext(taskFile))
    }

    val lesson = course.lessons[0]
    assertEquals(3, lesson.taskList.size)
    assertEquals(1, lesson.getTask("task1")!!.index)
    assertEquals(2, lesson.getTask("task01")!!.index)
    assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task before task`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
      testAction(CCCreateTask.ACTION_ID, dataContext(taskFile))
    }

    val lesson = course.lessons[0]
    assertEquals(3, lesson.taskList.size)
    assertEquals(1, lesson.getTask("task1")!!.index)
    assertEquals(2, lesson.getTask("task01")!!.index)
    assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task before task with custom name`() {
    val customTaskName = "Custom Task Name"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
      testAction(CCCreateTask.ACTION_ID, dataContext(taskFile))
    }

    val lesson = course.lessons[0]
    assertEquals(3, lesson.taskList.size)
    assertEquals(1, lesson.getTask("task1")!!.index)
    assertEquals(2, lesson.getTask("task01")!!.index)
    assertEquals(3, lesson.getTask(customTaskName)!!.index)
  }

  fun `test create task after task in section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
      testAction(CCCreateTask.ACTION_ID, dataContext(taskFile))
    }

    val lesson = course.sections[0].lessons[0]
    assertEquals(3, lesson.taskList.size)
    assertEquals(1, lesson.getTask("task1")!!.index)
    assertEquals(2, lesson.getTask("task01")!!.index)
    assertEquals(3, lesson.getTask("task2")!!.index)
  }

  fun `test create task not available on course`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    val sourceVFile = LightPlatformTestCase.getSourceRoot()!!
    testAction(CCCreateTask.ACTION_ID, dataContext(sourceVFile), shouldBeEnabled = false)
  }

  fun `test create task not available on section`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    val sourceVFile = findFile("section1")
    testAction(CCCreateTask.ACTION_ID, dataContext(sourceVFile), shouldBeEnabled = false)
  }

  fun `test create framework task without test copy`() = doCreateFrameworkTaskTest(false)

  fun `test create framework task with test copy`() = doCreateFrameworkTaskTest(true)

  private fun doCreateFrameworkTaskTest(copyTests: Boolean) {
    val lessonName = "lesson1"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      frameworkLesson(lessonName) {
        eduTask("task1") {
          taskFile("src/Task.kt")
          taskFile("test/Tests1.kt", "//text changed", visible = false)
        }
      }
    }

    val lessonFile = findFile(lessonName)
    val newTaskName = "task2"
    withMockCreateStudyItemUi(MockNewStudyItemUi(newTaskName)) {
      withSettingsValue(CCSettings.getInstance()::copyTestsInFrameworkLessons, copyTests) {
        testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
      }
    }

    val newTask = course.findTask(lessonName, newTaskName)
    val copyTestFileMatcher = hasItem("test/Tests1.kt")
    val notCopyTestFileMatcher = hasItem("test/Tests.kt")
    val testFileMatcher = if (copyTests) copyTestFileMatcher else notCopyTestFileMatcher

    assertThat(newTask.taskFiles.keys, allOf(hasItem("src/Task.kt"), testFileMatcher))
    val testText = if (copyTests) "//text changed" else getDefaultTestText(course)
    assertEquals(testText, newTask.taskFiles[if (copyTests) "test/Tests1.kt" else "test/Tests.kt"]?.text)

    fileTree {
      dir(lessonName) {
        dir("task1") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests1.kt")
          }
          file("task.md")
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
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test create task suggest name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {}
    }

    fun assertTasks(vararg names: String) {
      val actualNames = course.lessons[0].taskList.map { it.name }
      assertEquals(listOf(*names), actualNames)
    }

    fun createTask(contextFolder: String, suggestedIndex: Int? = null, suggestedName: String? = null) {
      withMockCreateStudyItemUi(MockNewStudyItemUi(suggestedName, suggestedIndex)) {
        testAction(CCCreateTask.ACTION_ID, dataContext(findFile(contextFolder)))
      }
    }

    createTask("lesson1")
    assertTasks("task1")

    createTask("lesson1", suggestedName = "abc")
    assertTasks("task1", "abc")

    createTask("lesson1", suggestedName = "task3")
    assertTasks("task1", "abc", "task3")

    createTask("lesson1") // create a new task at the end
    assertTasks("task1", "abc", "task3", "task4")

    createTask("lesson1/task3") // create a new task after task3
    assertTasks("task1", "abc", "task3", "task5", "task4")

    createTask("lesson1/task3")
    assertTasks("task1", "abc", "task3", "task6", "task5", "task4")

    createTask("lesson1")
    assertTasks("task1", "abc", "task3", "task6", "task5", "task4", "task7")

    createTask("lesson1/abc")
    assertTasks("task1", "abc", "task2", "task3", "task6", "task5", "task4", "task7")

    createTask("lesson1", 2, "inserted-task")
    assertTasks("task1", "inserted-task", "abc", "task2", "task3", "task6", "task5", "task4", "task7")
  }

  private fun getDefaultTestText(course: Course): String? {
    val testTemplateName = course.configurator?.courseBuilder?.testTemplateName(course) ?: return null
    return GeneratorUtils.getInternalTemplateText(testTemplateName)
  }

  private fun checkOpenedFiles(task: Task) {
    val taskDir = task.getDir(myFixture.project.courseDir)!!
    val openFiles = FileEditorManagerEx.getInstanceEx(myFixture.project).openFiles
    openFiles.forEach { openFile ->
      assertTrue(VfsUtil.isAncestor(taskDir, openFile, true))
    }
    assertContainsElements(openFiles.toList(), task.getAllTestVFiles(myFixture.project))
    assertContainsElements(openFiles.toList(), task.getDescriptionFile(project))
  }
}
