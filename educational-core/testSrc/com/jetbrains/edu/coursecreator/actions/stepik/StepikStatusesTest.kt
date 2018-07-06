package com.jetbrains.edu.coursecreator.actions.stepik

import com.google.common.collect.Lists
import com.intellij.ide.actions.DeleteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCVirtualFileListener
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.create.CCTestCreateSection
import com.jetbrains.edu.coursecreator.actions.delete.CCDeleteActionTest
import com.jetbrains.edu.coursecreator.handlers.CCLessonMoveHandlerDelegate
import com.jetbrains.edu.coursecreator.handlers.CCSectionMoveHandlerDelegate
import com.jetbrains.edu.coursecreator.handlers.CCSectionRenameHandler
import com.jetbrains.edu.coursecreator.handlers.CCTaskMoveHandlerDelegate
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import junit.framework.TestCase

class StepikStatusesTest: CCActionTestCase() {

  private lateinit var ccVirtualFileListener : VirtualFileListener

  override fun setUp() {
    super.setUp()
    ccVirtualFileListener = CCVirtualFileListener(project)
    VirtualFileManager.getInstance().addVirtualFileListener(ccVirtualFileListener)
  }

  fun `test course up to date`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
      section("section2") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }
    setCourseAndInit(course)

    checkStatus(StudyTaskManager.getInstance(project).course!!, StepikChangeStatus.UP_TO_DATE)
  }

  fun `test course section added`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }
    setCourseAndInit(course)

    val section2 = findFile("section1")
    testAction(dataContext(section2), CCTestCreateSection("section2", 2))

    val currentCourse = StudyTaskManager.getInstance(project).course!!
    checkStatus(StudyTaskManager.getInstance(project).course!!, StepikChangeStatus.CONTENT)
    checkOtherItemsUpToDate(currentCourse, currentCourse)
  }

  fun `test course section deleted`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
      section("section2") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }
    setCourseAndInit(course)

    val section2 = findFile("section2")
    withTestDialog(EduTestDialog()) {
      testAction(dataContext(section2), DeleteAction())
    }

    val currentCourse = StudyTaskManager.getInstance(project).course!!
    checkStatus(currentCourse, StepikChangeStatus.CONTENT)
    checkOtherItemsUpToDate(currentCourse, currentCourse)
  }

  fun `test course lesson added`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }
    setCourseAndInit(course)

    val projectDir = EduUtils.getCourseDir(project)
    withTestDialog(EduTestInputDialog("lesson2")) {
      testAction(dataContext(projectDir), CCCreateLesson())
    }

    val currentCourse = StudyTaskManager.getInstance(project).course!!
    checkStatus(currentCourse, StepikChangeStatus.CONTENT)
  }

  fun `test course lesson deleted`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt")
        }
      }
    }
    setCourseAndInit(course)

    val lesson = findFile("lesson1")
    withTestDialog(EduTestDialog()) {
      testAction(dataContext(lesson), DeleteAction())
    }

    val currentCourse = StudyTaskManager.getInstance(project).course!!
    checkStatus(currentCourse, StepikChangeStatus.CONTENT)
    checkOtherItemsUpToDate(currentCourse, currentCourse)
  }

  fun `test course lesson moved`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      section {
        lesson("lesson2")
      }
    }

    setCourseAndInit(course)

    val sourceVFile = findFile("lesson1")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section2")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCLessonMoveHandlerDelegate()
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})

    val currentCourse = StudyTaskManager.getInstance(project).course!!
    checkStatus(currentCourse, StepikChangeStatus.CONTENT)

    val changedSection = currentCourse.sections[0]!!
    checkStatus(changedSection, StepikChangeStatus.INFO_AND_CONTENT)

    val changedLesson = currentCourse.sections[0].getLesson("lesson1")!!
    checkStatus(changedLesson, StepikChangeStatus.INFO)

    checkOtherItemsUpToDate(currentCourse, currentCourse, changedSection, changedLesson)
  }

  fun `test section renamed`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }
    setCourseAndInit(course)

    val section = findFile("section1")

    Messages.setTestInputDialog { "section2" }
    val dataContext = dataContext(section)
    val renameHandler = CCSectionRenameHandler()
    TestCase.assertNotNull(renameHandler)
    renameHandler.invoke(project, null, null, dataContext)


    val currentCourse = StudyTaskManager.getInstance(project).course!!

    val changedSection = currentCourse.sections[0]
    checkStatus(changedSection, StepikChangeStatus.INFO)

    checkOtherItemsUpToDate(currentCourse, changedSection)
  }

  fun `test section moved`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section()
      section()
    }
    StudyTaskManager.getInstance(project).course = convertToRemoteCourse(course)
    val currentCourse = StudyTaskManager.getInstance(project).course!!
    currentCourse.init(null, null, true)

    val sourceVFile = findFile("section2")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section1")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCSectionMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})

    val firstChangedSection = currentCourse.sections[0]
    checkStatus(firstChangedSection, StepikChangeStatus.INFO)

    val secondChangedSection = currentCourse.sections[1]
    checkStatus(secondChangedSection, StepikChangeStatus.INFO)

    checkOtherItemsUpToDate(currentCourse, firstChangedSection, secondChangedSection)
  }

  fun `test section moved to the top`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section()
      section()
      section()
      section()
      section()
      section()
    }
    setCourseAndInit(course)

    val sourceVFile = findFile("section6")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section1")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCSectionMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})

    val currentCourse = StudyTaskManager.getInstance(project).course!!

    currentCourse.sections.forEach {
      checkStatus(it, StepikChangeStatus.INFO)
    }
  }

  fun `test section moved in the middle`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section()
      section()
      section()
      section()
      section()
      section()
    }
    setCourseAndInit(course)

    val sourceVFile = findFile("section4")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section3")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCSectionMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})

    val currentCourse = StudyTaskManager.getInstance(project).course!!

    currentCourse.sections.sortBy { it.index }
    val changedSections = currentCourse.sections.subList(2, 6)
    changedSections.forEach {
      checkStatus(it, StepikChangeStatus.INFO)
    }

    checkOtherItemsUpToDate(currentCourse, *changedSections.toTypedArray())
  }

  fun `test lesson added into section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1"){
        lesson {
          eduTask()
        }
        lesson {
          eduTask()
        }
      }
    }
    setCourseAndInit(course)

    val currentCourse = StudyTaskManager.getInstance(project).course

    Messages.setTestInputDialog { "lesson3" }
    val sectionDir = EduUtils.getCourseDir(project).findChild(currentCourse!!.sections[0].name)
    testAction(dataContext(sectionDir!!), CCCreateLesson())

    val changedSection = currentCourse.sections[0]
    checkStatus(changedSection, StepikChangeStatus.CONTENT)

    checkOtherItemsUpToDate(currentCourse, changedSection)
  }

  fun `test lesson deleted from section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1"){
        lesson("lesson1")
        lesson("lesson2")
      }
    }
    setCourseAndInit(course)

    val lessonToDelete = findFile("section1/lesson2")
    withTestDialog(EduTestDialog()) {
      testAction(dataContext(lessonToDelete), DeleteAction())
    }

    val currentCourse = StudyTaskManager.getInstance(project).course!!
    val changedSection = currentCourse.sections[0]
    checkStatus(changedSection, StepikChangeStatus.CONTENT)

    checkOtherItemsUpToDate(currentCourse, changedSection)
  }

  fun `test lesson moved between sections`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1")
      }
      section("section2") {
        lesson("lesson1")
        lesson("lesson2")
      }
    }
    setCourseAndInit(course)

    val sourceVFile = findFile("section2/lesson2")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("section1")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCLessonMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})

    val currentCourse = StudyTaskManager.getInstance(project).course!!

    currentCourse.sections.sortBy { it.index }
    currentCourse.sections.forEach {
      checkStatus(it, StepikChangeStatus.CONTENT)
    }

    checkOtherItemsUpToDate(currentCourse, *currentCourse.sections.toTypedArray())
  }

  fun `test task added`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask()
        }
      }
    }
    setCourseAndInit(course)

    val lessonDir = findFile("section1/lesson1")
    withTestDialog(EduTestInputDialog("task2")) {
      testAction(dataContext(lessonDir), CCCreateTask())
    }

    val currentCourse = StudyTaskManager.getInstance(project).course!!
    val changedLesson = currentCourse.sections[0].lessons[0]
    checkStatus(changedLesson, StepikChangeStatus.CONTENT)
    checkOtherItemsUpToDate(currentCourse, changedLesson)
  }

  fun `test task deleted`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask()
          eduTask()
        }
      }
    }
    setCourseAndInit(course)

    val lessonDir = findFile("section1/lesson1/task1")
    val testDialog = CCDeleteActionTest.TestDeleteDialog()
    withTestDialog(testDialog) {
      testAction(dataContext(lessonDir), DeleteAction())
    }

    val currentCourse = StudyTaskManager.getInstance(project).course!!
    val changedLesson = currentCourse.sections[0].lessons[0]
    checkStatus(changedLesson, StepikChangeStatus.CONTENT)
    checkOtherItemsUpToDate(currentCourse, changedLesson)
  }

  fun `test task moved between lessons`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1")
        eduTask("task2")
      }

      lesson("lesson2") {
        eduTask("task1")
        eduTask("task3")
      }
    }
    setCourseAndInit(course)

    val sourceVFile = findFile("lesson2/task3")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson1")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCTaskMoveHandlerTest(0)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})

    val currentCourse = StudyTaskManager.getInstance(project).course!!

    currentCourse.lessons.forEach {
      checkStatus(it, StepikChangeStatus.CONTENT)
    }

    checkOtherItemsUpToDate(currentCourse, *currentCourse.lessons.toTypedArray())
  }

  fun `test task moved inside lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1")
        eduTask("task2")
        eduTask("task3")
      }
    }
    setCourseAndInit(course)

    val sourceVFile = findFile("lesson1/task2")
    val sourceDir = PsiManager.getInstance(project).findDirectory(sourceVFile)
    val targetVFile = findFile("lesson1/task3")
    val targetDir = PsiManager.getInstance(project).findDirectory(targetVFile)

    val handler = CCTaskMoveHandlerTest(1)
    TestCase.assertTrue(handler.canMove(arrayOf(sourceDir), targetDir))
    handler.doMove(project, arrayOf(sourceDir), targetDir, {})

    val currentCourse = StudyTaskManager.getInstance(project).course!!

    val changedTask2 = currentCourse.lessons[0].getTask("task2")!!
    checkStatus(changedTask2, StepikChangeStatus.INFO_AND_CONTENT)

    val changedTask3 = currentCourse.lessons[0].getTask("task3")!!
    checkStatus(changedTask3, StepikChangeStatus.INFO_AND_CONTENT)

    checkOtherItemsUpToDate(currentCourse, changedTask2, changedTask3)
  }

  private fun checkStatus(changedItem: StudyItem, expectedStatus: StepikChangeStatus) {
    TestCase.assertTrue("Wrong status ${changedItem.name}. Expected: $expectedStatus. Actual: ${changedItem.stepikChangeStatus}",
                        changedItem.stepikChangeStatus == expectedStatus)
  }

  private fun checkOtherItemsUpToDate(course: Course, vararg changedItems: StudyItem) {
    if (course !in changedItems) {
      TestCase.assertTrue("Wrong status for ${course.name}.Expected status: UP_TO_DATE. Actual: ${course.stepikChangeStatus}",
                          course.stepikChangeStatus == StepikChangeStatus.UP_TO_DATE)
    }

    for (item in course.items) {
      if (item in changedItems) {
        continue
      }

      if (item is Section) {
        TestCase.assertTrue("Wrong status for ${item.name}.Expected status: UP_TO_DATE. Actual: ${item.stepikChangeStatus}",
                            item.stepikChangeStatus == StepikChangeStatus.UP_TO_DATE)
        for (lesson in item.lessons) {
          checkLessonsUpToDate(lesson, changedItems)
        }
      }

      if (item is Lesson) {
        checkLessonsUpToDate(item, changedItems)
      }
    }

  }

  private fun checkLessonsUpToDate(lesson: Lesson,
                                   changedItems: Array<out StudyItem>) {
    if (lesson in changedItems) {
      return
    }

    TestCase.assertTrue("Wrong status for ${lesson.name}.Expected status: UP_TO_DATE. Actual: ${lesson.stepikChangeStatus}",
                        lesson.stepikChangeStatus == StepikChangeStatus.UP_TO_DATE)

    for (task in lesson.taskList) {
      if (task in changedItems) {
        continue
      }
      TestCase.assertTrue("Wrong status for ${task.name}.Expected status: UP_TO_DATE. Actual: ${task.stepikChangeStatus}",
                          task.stepikChangeStatus == StepikChangeStatus.UP_TO_DATE)
    }
  }

  private fun setCourseAndInit(course: Course) {
    StudyTaskManager.getInstance(project).course = convertToRemoteCourse(course)
    StudyTaskManager.getInstance(project).course!!.init(null, null, true)
  }


  private fun convertToRemoteCourse(course: Course): RemoteCourse {
    val remoteCourse = RemoteCourse()
    remoteCourse.id = 1
    remoteCourse.name = course.name
    remoteCourse.courseMode = CCUtils.COURSE_MODE
    remoteCourse.items = Lists.newArrayList(course.items)
    remoteCourse.language = course.language

    for (item in remoteCourse.items) {
      if (item is Section) {
        item.id = 1
        for (lesson in item.lessons) {
          lesson.id = 1
          for (task in lesson.taskList) {
            task.stepId = 1
          }
        }
      }

      if (item is Lesson) {
        item.id = 1
        for (task in item.taskList) {
          task.stepId = 1
        }
      }
    }
    return remoteCourse
  }


  internal inner class CCSectionMoveHandlerTest(private val myDelta: Int) : CCSectionMoveHandlerDelegate() {
    override fun getDelta(project: Project, targetItem: StudyItem): Int {
      return myDelta
    }
  }

  internal inner class CCLessonMoveHandlerTest(private val myDelta: Int) : CCLessonMoveHandlerDelegate() {
    override fun getDelta(project: Project, targetItem: StudyItem): Int {
      return myDelta
    }
  }

  internal inner class CCTaskMoveHandlerTest(private val myDelta: Int) : CCTaskMoveHandlerDelegate() {
    override fun getDelta(project: Project, targetItem: StudyItem): Int {
      return myDelta
    }
  }

}

