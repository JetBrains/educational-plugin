package com.jetbrains.edu.aiHints.core

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.aiHints.core.context.AuthorSolutionContext
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.authorSolutionContext
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.EduAIHintsUtils
import com.jetbrains.edu.learning.actions.EduAIHintsUtils.GET_HINT_ACTION_ID
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class GetHintTest : EduActionTestCase() {
  override fun setUp() {
    super.setUp()
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("task.txt")
        }
      }
    }
  }

  @Test
  fun `GetHint action NOT available by default`() {
    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action NOT available for non-marketplace student course`() {
    // when only
    acceptAgreement()
    registerPlainTextEduAiHintsProcessor(testRootDisposable)

    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action NOT available for marketplace student course with edu task`() {
    // when only
    acceptAgreement()
    getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)

    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action available for marketplace student course with code task`() {
    // when
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
    selectCurrentEduTask(course)
    initializeAuthorSolutionContext(course)

    // but
    HintStateManager.getInstance(project).acceptHint()

    // then
    testGetHintAction(shouldBeEnabled = true, shouldBeVisible = false)
    assertTrue(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action NOT available due to missing processor`() {
    // when only
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    selectCurrentEduTask(course)

    // then
    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action is still available when missing AI agreement`() {
    // when only
    val course = getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
    selectCurrentEduTask(course)
    initializeAuthorSolutionContext(course)

    // then
    testGetHintAction(shouldBeEnabled = true, shouldBeVisible = true)
    assertTrue(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action is not available for tasks with no functions`() {
    // when
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
    selectCurrentEduTask(course)

    // then
    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action available`() {
    // when
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
    selectCurrentEduTask(course)
    initializeAuthorSolutionContext(course)

    // then
    testGetHintAction(shouldBeEnabled = true, shouldBeVisible = true)
    assertTrue(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  private fun acceptAgreement() {
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED,
        UserAgreementState.ACCEPTED
      )
    )
  }

  private fun selectCurrentEduTask(course: Course) {
    val task = course.findTask("lesson1", "task1")
    task.status = CheckStatus.Failed
    val taskVirtualFile = task.getTaskFile("task.txt")?.getVirtualFile(project) ?: error("Virtual File for task is not found")
    FileEditorManager.getInstance(project).openFile(taskVirtualFile, true)
  }

  private fun initializeAuthorSolutionContext(course: Course) {
    val task = course.findTask("lesson1", "task1")
    val functionSignatures = setOf(FunctionSignature("foo", listOf(), "kotlin/String"))
    task.authorSolutionContext = AuthorSolutionContext(
      mapOf("foo" to listOf()),
      functionSignatures
    )
  }

  private fun testGetHintAction(shouldBeEnabled: Boolean, shouldBeVisible: Boolean) {
    testAction(GET_HINT_ACTION_ID, shouldBeEnabled = shouldBeEnabled, shouldBeVisible = shouldBeVisible, runAction = false)
  }
}