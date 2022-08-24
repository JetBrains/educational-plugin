package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.checker.PlaintTextCheckerFixture
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.stepik.api.MockStepikBasedConnector
import com.jetbrains.edu.learning.stepik.hyperskill.getPathWithoutPrams
import org.intellij.lang.annotations.Language
import java.util.*

abstract class StepikBasedCheckAnswerTaskTest : CheckersTestBase<Unit>() {
  protected abstract val defaultResponseCode: Int

  protected abstract val mockConnector: MockStepikBasedConnector

  override fun createCheckerFixture(): EduCheckerFixture<Unit> = PlaintTextCheckerFixture()

  protected fun configureResponses(succeed: Boolean) {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.getPathWithoutPrams()) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          "/api/submissions/11" -> if (succeed) {
            submissionWithSucceedStatus
          }
          else {
            submissionWithFailedStatus
          }
          else -> error("Wrong path: ${path}")
        },
        responseCode = defaultResponseCode
      )
    }
  }

  protected fun getSavedTextInFile(task: Task, fileName: String, savedText: String, project: Project): String {
    val taskFile = task.getTaskFile(fileName) ?: error("Task file with name: $fileName is absent")
    val document = taskFile.getDocument(project) ?: error(
      "Document from task file is null. File name is $fileName, task file is ${task.name}")
    runWriteAction {
      document.setText(savedText)
      FileDocumentManager.getInstance().saveDocument(document)
    }
    return document.text
  }

  protected fun testWithEnabledEnsureNewLineAtEOFSetting(testFunction: () -> Unit) {
    val initialValue = EditorSettingsExternalizable.getInstance().isEnsureNewLineAtEOF
    try {
      EditorSettingsExternalizable.getInstance().isEnsureNewLineAtEOF = true
      testFunction()
    }
    finally {
      EditorSettingsExternalizable.getInstance().isEnsureNewLineAtEOF = initialValue
    }
  }

  @Language("JSON")
  protected val attempt = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": { },
          "id": 11,
          "status": "active",
          "step": 1,
          "time": "${Date().format()}",
          "user": 1,
          "time_left": null
        }
      ]
    }
  """

  @Language("JSON")
  protected val submission = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "11",
          "id": "11",
          "status": "evaluation",
          "step": 1,
          "time": "${Date().format()}",
          "user": 1
        }
      ]
    }
  """

  @Language("JSON")
  protected val submissionWithSucceedStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "11",
          "id": "11",
          "status": "succeed",
          "step": 1,
          "time": "${Date().format()}",
          "user": 1
        }
      ]
    }
  """

  @Language("JSON")
  protected val submissionWithFailedStatus = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "11",
          "id": "11",
          "status": "wrong",
          "step": 1,
          "time": "${Date().format()}",
          "user": 1
        }
      ]
    }
  """

  companion object {
    @JvmStatic
    protected val SECTION: String = "Section"
    @JvmStatic
    protected val LESSON: String = "Lesson"
  }
}