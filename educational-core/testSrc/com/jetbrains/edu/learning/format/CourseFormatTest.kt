package com.jetbrains.edu.learning.format

import com.intellij.openapi.util.Pair
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.stepik.StepikUserInfo

class CourseFormatTest : EduTestCase() {
  fun testAdditionalMaterialsLesson() {
    val course = courseFromJson
    assertNotNull(course.additionalFiles)
    assertFalse(course.additionalFiles.isEmpty())
    assertEquals("test_helper.py", course.additionalFiles[0].name)
  }

  fun testCourseWithSection() {
    val course = courseFromJson
    val items = course.items
    assertEquals(2, items.size)
    assertTrue(items[0] is Section)
    assertTrue(items[1] is Lesson)
    assertEquals(1, (items[0] as Section).lessons.size)
  }

  fun testFrameworkLesson() {
    val course = courseFromJson
    val items = course.items
    assertEquals(1, items.size)
    assertTrue(items[0] is FrameworkLesson)
  }

  fun testPycharmToEduTask() {
    val course = courseFromJson
    val lessons = course.lessons
    assertFalse("No lessons found", lessons.isEmpty())
    val lesson = lessons[0]
    val taskList = lesson.taskList
    assertFalse("No tasks found", taskList.isEmpty())
    assertTrue(taskList[0] is EduTask)
  }

  fun testDescription() {
    val eduTask = firstEduTask
    assertEquals("First task description", EduUtils.getTaskTextFromTask(project, eduTask))
  }

  fun testFeedbackLinks() {
    val eduTask = firstEduTask
    val feedbackLink = eduTask.feedbackLink
    assertEquals(FeedbackLink.LinkType.CUSTOM, feedbackLink.type)
    assertEquals("https://www.jetbrains.com/", feedbackLink.link)
  }

  fun testPlaceholderText() {
    val eduTask = firstEduTask
    val taskFile = eduTask.getTaskFile("task.py")
    check(taskFile != null)
    val answerPlaceholders = taskFile.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("write function body", answerPlaceholders[0].placeholderText)
  }

  fun testPossibleAnswer() {
    val eduTask = firstEduTask
    val taskFile = eduTask.getTaskFile("task.py")
    check(taskFile != null)
    val answerPlaceholders = taskFile.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("pass", answerPlaceholders[0].possibleAnswer)
  }

  fun testCourseName() {
    val course = courseFromJson
    assertEquals("My Python Course", course.name)
  }

  fun testCourseProgrammingLanguage() {
    val course = courseFromJson
    assertEquals(EduNames.PYTHON, course.languageID)
  }

  fun testCourseLanguage() {
    val course = courseFromJson
    assertEquals("Russian", course.humanLanguage)
  }

  fun testCourseDescription() {
    val course = courseFromJson
    assertEquals("Best course ever", course.description)
  }

  fun testStudentTaskText() {
    val course = courseFromJson
    val lessons = course.lessons
    assertFalse("No lessons found", lessons.isEmpty())
    val lesson = lessons[0]
    val taskList = lesson.taskList
    assertFalse("No tasks found", taskList.isEmpty())
    val task = taskList[0]
    val taskFile = task.getTaskFile("my_task.py")
    assertNotNull(taskFile)
    assertEquals("def foo():\n    write function body\n", taskFile!!.text)
  }

  fun testChoiceTasks() {
    val course = courseFromJson
    val task = course.lessons[0].taskList[0]
    check(task is ChoiceTask)
    assertTrue(task.isMultipleChoice)
    val choiceOptions = task.choiceOptions
    val actualChoiceOptions = ContainerUtil.newHashMap(ContainerUtil.map(choiceOptions) { it.text },
                                                       ContainerUtil.map(choiceOptions) { it.status })
    assertEquals(ContainerUtil.newHashMap(Pair.create("1", ChoiceOptionStatus.CORRECT),
                                          Pair.create("2", ChoiceOptionStatus.INCORRECT)),
                 actualChoiceOptions)
  }

  fun testCourseWithAuthors() {
    val course = courseFromJson
    assertEquals(ContainerUtil.newArrayList("EduTools Dev", "EduTools QA", "EduTools"),
                 ContainerUtil.map(course.authors) { info: StepikUserInfo -> info.name })
  }

  fun testSolutionsHiddenInCourse() {
    val course = courseFromJson
    assertTrue(course.solutionsHidden)
  }

  fun testSolutionHiddenInTask() {
    val course = courseFromJson
    val task = course.lessons[0].taskList[0]
    assertTrue(task.solutionHidden!!)
  }

  private val courseFromJson: Course
    get() {
      val fileName = testFile
      return createCourseFromJson(testDataPath + fileName, CourseMode.STUDENT)
    }

  override fun getTestDataPath(): String = "${super.getTestDataPath()}/format/"

  private val testFile: String get() = "${getTestName(true)}.json"

  private val firstEduTask: EduTask
    get() {
      val course = courseFromJson
      course.init(null, null, false)
      course.createCourseFiles(project, module, LightPlatformTestCase.getSourceRoot(), Any())
      val lessons = course.lessons
      assertFalse("No lessons found", lessons.isEmpty())
      val lesson = lessons[0]
      val taskList = lesson.taskList
      assertFalse("No tasks found", taskList.isEmpty())
      val task = taskList[0]
      check(task is EduTask)
      return task
    }
}