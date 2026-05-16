package com.jetbrains.edu.learning.actions

import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.chains.SimpleDiffRequestChain.DiffRequestProducerWrapper
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class CompareWithAnswerActionTest : EduActionTestCase() {

  @Test
  fun `test disabled in educator mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("task.txt", "task file text")
        }
      }
    }

    doTestDisabled()
  }

  @Test
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
      override fun showSolution(project: Project, diffRequestChain: SimpleDiffRequestChain) {
        val diffReq = diffRequestChain.requests[0] as DiffRequestProducerWrapper
        val answerContent = (diffReq.request as SimpleDiffRequest).contents[1] as DocumentContent
        assertEquals("task file text answer", answerContent.document.text)
      }
    })
  }

  private fun doTestDisabled() {
    openFirstTaskFile()

    testAction(CompareWithAnswerAction.ACTION_ID, shouldBeEnabled = false)
  }

  private fun openFirstTaskFile() {
    val task = findTask(0, 0).apply { status = CheckStatus.Solved }
    task.openTaskFileInEditor("task.txt")
  }
}