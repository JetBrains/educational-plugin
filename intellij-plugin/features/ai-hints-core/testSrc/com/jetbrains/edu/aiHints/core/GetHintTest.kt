package com.jetbrains.edu.aiHints.core

import com.intellij.ide.HelpTooltip
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.components.ActionLink
import com.jetbrains.edu.aiHints.core.action.GetHint
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.EduAIHintsUtils
import com.jetbrains.edu.learning.actions.EduAIHintsUtils.GET_HINT_ACTION_ID
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import com.jetbrains.edu.learning.testAction
import org.junit.Test
import java.util.function.BooleanSupplier
import java.util.function.Supplier

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
    testHelpTooltipInstalled()
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action NOT available for non-marketplace student course`() {
    // when only
    acceptAgreement()
    registerPlainTextEduAiHintsProcessor(testRootDisposable)

    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    testHelpTooltipInstalled()
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action NOT available for marketplace student course with edu task`() {
    // when only
    acceptAgreement()
    getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)

    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    testHelpTooltipInstalled()
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action available for marketplace student course with code task`() {
    // when
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
    selectCurrentEduTask(course)

    // but
    HintStateManager.getInstance(project).acceptHint()

    // then
    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabledAndVisible())
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
    testHelpTooltipInstalled()
  }

  @Test
  fun `GetHint action NOT available due to missing processor`() {
    // when only
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    selectCurrentEduTask(course)

    // then
    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    testHelpTooltipInstalled()
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action NOT available due to missing AI agreement`() {
    // when only
    val course = getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
    selectCurrentEduTask(course)

    // then
    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = true)
    testHelpTooltipInstalled(shouldBeShown = true)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabledAndVisible())
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action available`() {
    // when
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
    selectCurrentEduTask(course)

    // then
    testGetHintAction(shouldBeEnabled = true, shouldBeVisible = true)
    testHelpTooltipInstalled()
    assertTrue(EduAIHintsUtils.getHintActionPresentation(project).isEnabledAndVisible())
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

  private fun testGetHintAction(shouldBeEnabled: Boolean, shouldBeVisible: Boolean) {
    testAction(GET_HINT_ACTION_ID, shouldBeEnabled = shouldBeEnabled, shouldBeVisible = shouldBeVisible, runAction = false)
  }

  private fun testHelpTooltipInstalled(shouldBeShown: Boolean = false) {
    val getHintAction = ActionManager.getInstance().getAction(GET_HINT_ACTION_ID) as GetHint
    // BACKCOMPAT: 2024.2 Replace with [AnActionEvent.createEvent]
    @Suppress("DEPRECATION", "removal")
    val anActionEvent = AnActionEvent.createFromInputEvent(
      null,
      CheckPanel.ACTION_PLACE,
      getHintAction.templatePresentation.clone(),
      SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
    )
    getHintAction.update(anActionEvent)
    val customComponent = getHintAction.createCustomComponent(anActionEvent.presentation, CheckPanel.ACTION_PLACE)
    val helpToolTip = HelpTooltip.getTooltipFor(customComponent)
    checkNotNull(helpToolTip)
    val title = (helpToolTip.javaClass.getDeclaredField("title").apply { isAccessible = true }.get(helpToolTip) as Supplier<*>).get()
    val description = helpToolTip.javaClass.getDeclaredField("description").apply { isAccessible = true }.get(helpToolTip)
    val actionLinkText = (helpToolTip.javaClass.getDeclaredField("link").apply { isAccessible = true }.get(helpToolTip) as ActionLink).text
    val masterPopupOpenCondition = helpToolTip.javaClass.getDeclaredField("masterPopupOpenCondition").apply { isAccessible = true }.get(helpToolTip) as BooleanSupplier
    assertEquals(shouldBeShown, masterPopupOpenCondition.asBoolean)
    assertEquals(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.text"), title)
    assertEquals(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.tooltip.description"), description)
    assertEquals(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.tooltip.link"), actionLinkText)
  }
}