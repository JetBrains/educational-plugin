package com.jetbrains.edu.learning.format

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.courseFormat.ext.getTaskTextFromTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.createCourseFiles
import com.jetbrains.edu.learning.createCourseFromJson

class CourseFormatTest : EduTestCase() {
  fun testAdditionalMaterialsLesson() {
    assertNotNull(courseFromJson.additionalFiles)
    assertFalse(courseFromJson.additionalFiles.isEmpty())
    assertEquals("test_helper.py", courseFromJson.additionalFiles[0].name)
  }

  fun testCourseWithSection() {
    val items = courseFromJson.items
    assertEquals(2, items.size)
    assertTrue(items[0] is Section)
    assertTrue(items[1] is Lesson)
    assertEquals(1, (items[0] as Section).lessons.size)
  }

  fun testFrameworkLesson() {
    assertEquals(1, courseFromJson.items.size)
    val lesson = courseFromJson.items[0]
    check(lesson is FrameworkLesson)
    assertTrue(lesson.isTemplateBased)
  }

  fun testNonTemplateBasedFrameworkLesson() {
    assertEquals(1, courseFromJson.items.size)
    val lesson = courseFromJson.items[0]
    check(lesson is FrameworkLesson)
    assertFalse(lesson.isTemplateBased)
  }

  fun testPycharmToEduTask() {
    val lessons = courseFromJson.lessons
    assertFalse("No lessons found", lessons.isEmpty())
    val lesson = lessons[0]
    val taskList = lesson.taskList
    assertFalse("No tasks found", taskList.isEmpty())
    assertTrue(taskList[0] is EduTask)
  }

  fun testDescription() {
    assertEquals("First task description", firstEduTask.getTaskTextFromTask(project))
  }

  fun testFeedbackLinks() {
    assertEquals("https://www.jetbrains.com/", firstEduTask.feedbackLink)
  }

  fun testPlaceholderText() {
    val taskFile = firstEduTask.getTaskFile("task.py")
    check(taskFile != null)
    val answerPlaceholders = taskFile.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("write function body", answerPlaceholders[0].placeholderText)
  }

  fun testPossibleAnswer() {
    val taskFile = firstEduTask.getTaskFile("task.py")
    check(taskFile != null)
    val answerPlaceholders = taskFile.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("pass", answerPlaceholders[0].possibleAnswer)
  }

  fun testCourseName() {
    assertEquals("My Python Course", courseFromJson.name)
  }

  fun testCourseOldProgrammingLanguage() {
    assertEquals(EduFormatNames.PYTHON, courseFromJson.languageId)
    assertNull(courseFromJson.languageVersion)
  }

  fun testCourseOldProgrammingLanguageWithVersion() {
    assertEquals(EduFormatNames.PYTHON, courseFromJson.languageId)
    assertEquals(PYTHON_3_VERSION, courseFromJson.languageVersion)
  }

  fun testCourseProgrammingLanguageId() {
    assertEquals(EduFormatNames.PYTHON, courseFromJson.languageId)
    assertNull(courseFromJson.languageVersion)
  }

  fun testCourseProgrammingLanguageVersion() {
    assertEquals(EduFormatNames.PYTHON, courseFromJson.languageId)
    assertEquals(PYTHON_3_VERSION, courseFromJson.languageVersion)
  }

  fun testCourseLanguage() {
    assertEquals("Russian", courseFromJson.humanLanguage)
  }

  fun testCourseDescription() {
    assertEquals("Best course ever", courseFromJson.description)
  }

  fun testStudentTaskText() {
    val lessons = courseFromJson.lessons
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
    val task = courseFromJson.lessons[0].taskList[0]
    check(task is ChoiceTask)
    assertTrue(task.isMultipleChoice)
    val choiceOptions = task.choiceOptions

    val actualChoiceOptions = choiceOptions.associateBy({ it.text }, { it.status })
    assertEquals(mapOf(Pair("1", ChoiceOptionStatus.CORRECT), Pair("2", ChoiceOptionStatus.INCORRECT)), actualChoiceOptions)
  }

  fun testCourseWithAuthors() {
    assertEquals(listOf("EduTools Dev", "EduTools QA", "EduTools"),
                 courseFromJson.authors.map { info -> info.getFullName() })
  }

  fun testSolutionsHiddenInCourse() {
    assertTrue(courseFromJson.solutionsHidden)
  }

  fun testSolutionHiddenInTask() {
    val task = courseFromJson.lessons[0].taskList[0]
    assertTrue(task.solutionHidden!!)
  }

  fun testPlaceholderWithInvisibleDependency() = doTestPlaceholderAndDependencyVisibility(
    courseFromJson.lessons[0].taskList[0],
    expectedPlaceholderVisibility = false
  )

  fun testInvisiblePlaceholder() = doTestPlaceholderAndDependencyVisibility(
    courseFromJson.lessons[0].taskList[0],
    expectedPlaceholderVisibility = false
  )

  fun testVisiblePlaceholderAndInvisibleDependency() = doTestPlaceholderAndDependencyVisibility(
    courseFromJson.lessons[0].taskList[0],
    expectedPlaceholderVisibility = false
  )

  fun testLocalCourseWithPlugins() {
    val pluginDependencies = courseFromJson.pluginDependencies
    assertSize(1, pluginDependencies)

    with(pluginDependencies.first()) {
      assertEquals("testPluginId", stringId)
      assertEquals("1.0", minVersion)
      assertEquals(null, maxVersion)
    }
  }

  fun testCourseLanguageVersionEmpty() {
    val course = course {}
    course.languageId = "Python"
    assertNull(course.languageVersion)
  }

  fun testEnvironmentSettings() {
    assertEquals(
      mapOf(
        "example key 1" to "example value 1",
        "example key 2" to "example value 2"
      ),
      courseFromJson.environmentSettings
    )
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
      course.init(false)
      course.createCourseFiles(project, LightPlatformTestCase.getSourceRoot())
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