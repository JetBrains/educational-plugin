package com.jetbrains.edu.integration.stepik

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.postCourse
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.stepik.StepikConnector
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader.PROGRESS_ID_PREFIX
import com.jetbrains.edu.learning.stepik.api.StepikNewConnector
import java.util.concurrent.TimeUnit


class LoadSolutionsTest : StepikTestCase() {

  private val taskStatusErrorMessage = "Wrong task status, expected Solved"
  private val fileUpdateErrorMessage = "File text weren't updated"

  fun `test task statuses`() {
    StudyTaskManager.getInstance(project).course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt", "no placeholders")
        }
      }
    }
    postCourse(project, StudyTaskManager.getInstance(project).course!!)

    solveFirstTask()

    val task = firstTask(StudyTaskManager.getInstance(project).course)
    val progresses = listOf(PROGRESS_ID_PREFIX + task.stepId.toString() )

    var isSolved = false

    // we have to wait until stepik process our submission and set task status
    val startTime = System.currentTimeMillis()
    val waitTime: Long = 10000
    val endTime = startTime + waitTime
    while (System.currentTimeMillis() < endTime) {
      val taskStatuses = StepikNewConnector.taskStatuses(progresses)
      assertTrue(taskNumberMismatchMessage(1, taskStatuses!!.size), taskStatuses.size == 1)

      if (taskStatuses.firstOrNull() == true) {
        isSolved = true
        break
      }

      TimeUnit.MILLISECONDS.sleep(500)
    }

    assertTrue(taskStatusErrorMessage, isSolved)
  }

  private fun taskNumberMismatchMessage(expected: Int, actual: Int?) = "Unexpected number of tasks, expected: $expected, actual: $actual"

  fun `test tasks to update`() {
    StudyTaskManager.getInstance(project).course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt", "no placeholders")
        }
      }
    }

    postCourse(project, StudyTaskManager.getInstance(project).course!!)
    solveFirstTask()

    val course = StudyTaskManager.getInstance(project).course!! as EduCourse
    val task = firstTask(StudyTaskManager.getInstance(project).course)
    val remoteCourse = StepikNewConnector.getCourseInfo(course.id, true) as EduCourse
    StepikConnector.loadCourseStructure(remoteCourse)

    val tasksToUpdate = StepikSolutionsLoader.getInstance(project).tasksToUpdate(remoteCourse as Course)
    assertTrue(taskNumberMismatchMessage(1, tasksToUpdate!!.size), tasksToUpdate.size == 1)
    val taskToUpdate = tasksToUpdate[0]
    assert(taskToUpdate.status == task.status)
    assert(taskToUpdate.taskFiles.size == 1)
  }

  private fun doCheck(check: (TaskFile, TaskFile) -> Unit = { _, _ -> }) {
    val course = StudyTaskManager.getInstance(project).course!! as EduCourse
    val oldTask = firstTask(course)
    val oldTaskFile = oldTask.getTaskFile(getInitialFileName())
    val oldVirtualFile = EduUtils.findTaskFileInDir(oldTaskFile!!, oldTask.getTaskDir(project)!!)

    val remoteCourse = createCourseFromStepik(course)
    val task = firstTask(remoteCourse)
    val taskFile = task.getTaskFile(getInitialFileName())
    val virtualFile = EduUtils.findTaskFileInDir(taskFile!!, oldTask.getTaskDir(project)!!)

    StepikSolutionsLoader.getInstance(project).doLoadSolution(task, true)

    assertEquals(fileUpdateErrorMessage, VfsUtil.loadText(oldVirtualFile!!), VfsUtil.loadText(virtualFile!!))
    check(oldTaskFile, taskFile)
  }

  private fun firstTask(course: Course?) = course!!.lessons.first().taskList.first()

  private fun createCourseFromStepik(course: EduCourse): EduCourse? {
    val remoteCourse = StepikNewConnector.getCourseInfo(course.id, true) as EduCourse
    StepikConnector.loadCourseStructure(remoteCourse)
    remoteCourse.init(null, null, false)
    remoteCourse.language = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(project).course = remoteCourse

    val file = myFixture.findFileInTempDir(getInitialFileName())
    myFixture.configureFromExistingVirtualFile(file)
    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
    return remoteCourse
  }

  private fun checkPlaceholders(oldTaskFile: TaskFile,
                                taskFile: TaskFile) {
    for (pair in oldTaskFile.answerPlaceholders.zip(taskFile.answerPlaceholders)) {
      val oldPlaceholder = pair.first
      val newPlaceholder = pair.second
      assertEquals(placeholderErrorMessage("offset", oldPlaceholder.offset, newPlaceholder.offset),
                   oldPlaceholder.offset, newPlaceholder.offset)
      assertEquals(placeholderErrorMessage("length", oldPlaceholder.length, newPlaceholder.length),
                   oldPlaceholder.length, newPlaceholder.length)
      assertEquals(placeholderErrorMessage("text", oldPlaceholder.placeholderText, newPlaceholder.placeholderText),
                   oldPlaceholder.placeholderText, newPlaceholder.placeholderText)
      assertEquals(placeholderErrorMessage("real length", oldPlaceholder.realLength, newPlaceholder.realLength),
                   oldPlaceholder.realLength, newPlaceholder.realLength)
    }
  }

  private fun placeholderErrorMessage(comparedElementName: String,
                                      expected: Any,
                                      actual: Any) =
    "Placeholders $comparedElementName mismatch. Expected: $expected Actual: $actual"

  private fun solveFirstTask() {
    val task = firstTask(StudyTaskManager.getInstance(project).course)
    task.status = CheckStatus.Solved
    val taskFile = task.getTaskFile(getInitialFileName())
    val virtualFile = EduUtils.findTaskFileInDir(taskFile!!, task.getTaskDir(project)!!)
    val document = FileDocumentManager.getInstance().getDocument(virtualFile!!)
    for (answerPlaceholder in taskFile.answerPlaceholders) {
      EduUtils.replaceAnswerPlaceholder(document!!, answerPlaceholder, answerPlaceholder.realLength, answerPlaceholder.possibleAnswer)
    }

    StepikConnector.postSolution(task, true, project)
  }

  private fun getInitialFileName() = "fizz.kt"

  private fun getNameWithoutPrefix() = name.replaceFirst("test", "").decapitalize()

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/stepik/loadSolutions"
  }
}