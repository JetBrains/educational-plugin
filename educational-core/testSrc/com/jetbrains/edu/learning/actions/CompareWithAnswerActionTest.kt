package com.jetbrains.edu.learning.actions

import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.MockEduBrowser
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_SOLUTIONS_ANCHOR
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourseWithFiles
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillTaskLink
import com.jetbrains.edu.learning.testAction

class CompareWithAnswerActionTest : EduActionTestCase() {
  fun `test disabled for codeforces`() {
    courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeTask {
          taskFile("task.txt")
        }
      }
    }

    doTestDisabled()
  }

  fun `test for Hyperskill course`() {
    hyperskillCourseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("task.txt", "stage 1")
        }
        eduTask("task2", stepId = 2) {
          taskFile("task.txt", "stage 2")
        }
      }
    }
    openFirstTaskFile()

    testAction(CompareWithAnswerAction.ACTION_ID, dataContext(findFileInTask(0, 0, "task.txt")))

    val mockEduBrowser = EduBrowser.getInstance() as MockEduBrowser
    assertEquals("${hyperskillTaskLink(findTask(0, 0))}$HYPERSKILL_SOLUTIONS_ANCHOR", mockEduBrowser.lastVisitedUrl)
  }

  fun `test disabled in educator mode`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("task.txt", "task file text")
        }
      }
    }

    doTestDisabled()
  }

  fun `test solution shown`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "task file text <p>placeholder</p>") {
            placeholder(0, possibleAnswer = "answer")
          }
        }
      }
    }

    openFirstTaskFile()

    testAction(object : CompareWithAnswerAction() {
      override fun showSolution(project: Project, requests: List<SimpleDiffRequest>) {
        val answerContent = requests[0].contents[1] as DocumentContent
        assertEquals("task file text answer", answerContent.document.text)
      }
    }, dataContext(findFileInTask(0, 0, "task.txt")))
  }

  private fun doTestDisabled() {
    openFirstTaskFile()

    testAction(CompareWithAnswerAction.ACTION_ID, dataContext(findFileInTask(0, 0, "task.txt")), shouldBeEnabled = false)
  }

  private fun openFirstTaskFile() {
    val task = findTask(0, 0).apply { status = CheckStatus.Solved }
    task.openTaskFileInEditor("task.txt")
  }
}