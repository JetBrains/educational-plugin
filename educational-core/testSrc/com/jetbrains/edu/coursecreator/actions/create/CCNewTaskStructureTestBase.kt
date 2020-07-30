package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.fileTree

abstract class CCNewTaskStructureTestBase : EduActionTestCase() {

  protected abstract val language: Language
  protected abstract val settings: Any
  protected open val environment: String = ""
  protected open val courseProducer: () -> Course = ::EduCourse

  protected open fun createMockUi(taskName: String, taskType: String): MockNewStudyItemUi =
    MockNewStudyItemUi(taskName, itemType = taskType)

  protected fun checkEduTaskCreation(
    fullTaskStructure: FileTreeBuilder.() -> Unit,
    taskStructureWithoutSources: FileTreeBuilder.() -> Unit
  ) = doTest("Edu", EduTask::class.java, fullTaskStructure, taskStructureWithoutSources)

  protected fun checkOutputTaskCreation(
    fullTaskStructure: FileTreeBuilder.() -> Unit,
    taskStructureWithoutSources: FileTreeBuilder.() -> Unit
  ) = doTest("Output", OutputTask::class.java, fullTaskStructure, taskStructureWithoutSources)

  protected fun checkTheoryTaskCreation(
    fullTaskStructure: FileTreeBuilder.() -> Unit,
    taskStructureWithoutSources: FileTreeBuilder.() -> Unit
  ) = doTest("Theory", TheoryTask::class.java, fullTaskStructure, taskStructureWithoutSources)

  protected fun checkIdeTaskCreation(
    fullTaskStructure: FileTreeBuilder.() -> Unit,
    taskStructureWithoutSources: FileTreeBuilder.() -> Unit
  ) = doTest("IDE", IdeTask::class.java, fullTaskStructure, taskStructureWithoutSources)

  protected fun checkChoiceTaskCreation(
    fullTaskStructure: FileTreeBuilder.() -> Unit,
    taskStructureWithoutSources: FileTreeBuilder.() -> Unit
  ) {
    doTest("Multiple-Choice", ChoiceTask::class.java, fullTaskStructure, taskStructureWithoutSources)
    val choiceTask = getCourse().findTask("lesson1", "task1") as ChoiceTask
    assertEquals(2, choiceTask.choiceOptions.size)
    assertEquals("Correct", choiceTask.choiceOptions[0].text)
    assertEquals(ChoiceOptionStatus.CORRECT, choiceTask.choiceOptions[0].status)
    assertEquals("Incorrect", choiceTask.choiceOptions[1].text)
    assertEquals(ChoiceOptionStatus.INCORRECT, choiceTask.choiceOptions[1].status)
  }

  /**
   * Checks creation on new task with [taskType] in three different cases:
   * - new task in [Lesson]. Expected task file structure should be provided by [fullTaskStructure]
   * - new the first task in [FrameworkLesson]. Expected task file structure should be provided by [fullTaskStructure],
   * i.e. it should be the same as every new task in [Lesson]
   * - new non-first task in framework lesson without tests copy.
   * Expected task file structure should be provided by [taskStructureWithoutSources]
   */
  private fun doTest(
    taskType: String,
    taskClass: Class<out Task>,
    fullTaskStructure: FileTreeBuilder.() -> Unit,
    taskStructureWithoutSources: FileTreeBuilder.() -> Unit
  ) {
    val course = courseWithFiles(
      language = language,
      settings = settings,
      environment = environment,
      courseMode = CCUtils.COURSE_MODE,
      courseProducer = courseProducer
    ) {
      lesson("lesson1")
      frameworkLesson("lesson2") {
        eduTask("task2")
      }
    }

    fun checkTaskStructure(
      lessonName: String,
      taskName: String,
      taskStructure: FileTreeBuilder.() -> Unit
    ) {
      val task = course.findTask(lessonName, taskName)
      assertInstanceOf(task, taskClass)
      fileTree(taskStructure).assertEquals(task.getDir(project.courseDir)!!, myFixture)
    }

    val lessonFile = findFile("lesson1")
    withMockCreateStudyItemUi(createMockUi("task1", taskType)) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }
    checkTaskStructure("lesson1", "task1", fullTaskStructure)

    val frameworkLessonFile = findFile("lesson2")
    withMockCreateStudyItemUi(createMockUi("task3", taskType)) {
      val settings = CCSettings.getInstance()
      val copyTestOld = settings.copyTestsInFrameworkLessons()
      settings.setCopyTestsInFrameworkLessons(false)
      try {
        testAction(dataContext(frameworkLessonFile), CCCreateTask())
      }
      finally {
        settings.setCopyTestsInFrameworkLessons(copyTestOld)
      }
    }
    checkTaskStructure("lesson2", "task3", taskStructureWithoutSources)
  }
}
