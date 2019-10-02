package com.jetbrains.edu.integration.stepik

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.postCourse
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.createCourseFiles
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader.PROGRESS_ID_PREFIX
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader
import java.util.concurrent.TimeUnit

class LoadSolutionsTest : StepikTestCase() {

  fun `test task statuses`() {
    StudyTaskManager.getInstance(project).course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt", "no placeholders")
        }
      }
    }
    postCourse(project, StudyTaskManager.getInstance(project).course!!.asEduCourse())

    solveFirstTask()

    val task = firstTask(StudyTaskManager.getInstance(project).course)
    val progresses = listOf(PROGRESS_ID_PREFIX + task.id.toString() )

    var isSolved = false

    // we have to wait until stepik process our submission and set task status
    val startTime = System.currentTimeMillis()
    val waitTime: Long = 10000
    val endTime = startTime + waitTime
    while (System.currentTimeMillis() < endTime) {
      val taskStatuses = StepikConnector.getInstance().taskStatuses(progresses)!!
      assertEquals("Unexpected number of tasks", 1, taskStatuses.size)

      if (taskStatuses.firstOrNull() == true) {
        isSolved = true
        break
      }

      TimeUnit.MILLISECONDS.sleep(500)
    }

    assertTrue("Wrong task status, expected Solved", isSolved)
  }

  fun `test tasks to update`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask {
          taskFile("fizz.kt", "no placeholders")
        }
      }
    }

    postCourse(project, StudyTaskManager.getInstance(project).course!!.asEduCourse())
    solveFirstTask()

    val course = StudyTaskManager.getInstance(project).course!! as EduCourse
    val task = firstTask(StudyTaskManager.getInstance(project).course)
    val remoteCourse = StepikConnector.getInstance().getCourseInfo(course.id, true) as EduCourse
    StepikCourseLoader.loadCourseStructure(remoteCourse)
    remoteCourse.init(null, null, false)
    StudyTaskManager.getInstance(project).course = remoteCourse

    val tasksToUpdate = StepikSolutionsLoader.getInstance(project).tasksToUpdate(remoteCourse as Course)
    assertEquals("Unexpected number of tasks", 1, tasksToUpdate.size)
    val taskToUpdate = tasksToUpdate[0]
    assertEquals(task.status, taskToUpdate.status)
    assertEquals(1, taskToUpdate.taskFiles.size)
  }

  fun `test framework lesson solutions`() {
    val educatorCourse = courseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("fizz.kt", "fun fizz() {}")
        }
        eduTask("task2") {
          taskFile("fizz.kt", "fun fizz() {}")
        }
      }
    }
    postCourse(project, educatorCourse.asEduCourse())
    val remoteCourseId = StudyTaskManager.getInstance(project).course!!.id

    createStudentCourseFromStepik(remoteCourseId).run {
      val task1 = findTask("lesson1", "task1")
      task1.openTaskFileInEditor("fizz.kt")
      myFixture.type("// comment from task 1\n")
      task1.solve()
      myFixture.testAction(NextTaskAction())
      val task2 = findTask("lesson1", "task2")
      task2.openTaskFileInEditor("fizz.kt")
      myFixture.type("// comment from task 2\n")
      task2.solve()
    }

    val studentCourse = createStudentCourseFromStepik(remoteCourseId)
    StepikSolutionsLoader.getInstance(project).loadSolutions(null, studentCourse)

    UIUtil.dispatchAllInvocationEvents()

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            // comment from task 1
            fun fizz() {}
          """)
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)

    studentCourse.findTask("lesson1", "task1").openTaskFileInEditor("fizz.kt")
    myFixture.testAction(NextTaskAction())

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            // comment from task 2
            fun fizz() {}
          """)
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test framework lesson solutions with dependencies`() {
    val educatorCourse = courseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("fizz.kt", "fun fizz() = <p>\"Fizz\"</p>") {
            placeholder(0, placeholderText = "TODO()")
          }
        }
        eduTask("task2") {
          taskFile("fizz.kt", "fun fizz() = <p>\"Fizz\"</p>") {
            placeholder(0, placeholderText = "TODO()", dependency = "lesson1#task1#fizz.kt#1")
          }
        }
      }
    }
    postCourse(project, educatorCourse.asEduCourse())
    val remoteCourseId = StudyTaskManager.getInstance(project).course!!.id

    createStudentCourseFromStepik(remoteCourseId).run {
      val task1 = findTask("lesson1", "task1")
      task1.openTaskFileInEditor("fizz.kt")
      myFixture.editor.selectionModel.removeSelection()
      myFixture.editor.caretModel.moveToOffset(0)
      myFixture.type("// comment from task 1\n")
      task1.solve()
    }

    val studentCourse = createStudentCourseFromStepik(remoteCourseId)
    StepikSolutionsLoader.getInstance(project).loadSolutions(null, studentCourse)

    UIUtil.dispatchAllInvocationEvents()

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            // comment from task 1
            fun fizz() = "Fizz"
          """)
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)

    studentCourse.findTask("lesson1", "task1").openTaskFileInEditor("fizz.kt")
    myFixture.testAction(NextTaskAction())

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun fizz() = "Fizz"
          """)
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  private fun createStudentCourseFromStepik(courseId: Int): EduCourse {
    require(courseId > 0)
    cleanupCourseFiles()
    val studentCourse = StepikConnector.getInstance().getCourseInfo(courseId, true) as EduCourse
    StepikCourseLoader.loadCourseStructure(studentCourse)
    studentCourse.createCourseFiles(project)
    return studentCourse
  }

  private fun cleanupCourseFiles() {
    for (child in LightPlatformTestCase.getSourceRoot().children) {
      runWriteAction { child.delete(LoadSolutionsTest::class.java) }
    }
  }

  private fun firstTask(course: Course?): Task = course!!.lessons.first().taskList.first()

  private fun solveFirstTask() = firstTask(StudyTaskManager.getInstance(project).course).solve()

  private fun Task.solve() {
    status = CheckStatus.Solved
    val taskFile = getTaskFile(getInitialFileName())
    val virtualFile = EduUtils.findTaskFileInDir(taskFile!!, getTaskDir(project)!!)
    val document = FileDocumentManager.getInstance().getDocument(virtualFile!!)
    for (answerPlaceholder in taskFile.answerPlaceholders) {
      CCUtils.replaceAnswerPlaceholder(document!!, answerPlaceholder)
    }

    StepikSolutionsLoader.postSolution(this, true, project)
  }

  private fun getInitialFileName() = "fizz.kt"

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/stepik/loadSolutions"
  }
}
