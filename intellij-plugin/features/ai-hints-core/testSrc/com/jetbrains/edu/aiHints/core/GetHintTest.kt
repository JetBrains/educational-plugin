package com.jetbrains.edu.aiHints.core

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.api.*
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.aiHints.core.context.SignatureSource
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
    registerEduAiHintsProcessor()

    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action NOT available for marketplace student course with edu task`() {
    // when only
    acceptAgreement()
    getCourse().apply { isMarketplace = true }
    registerEduAiHintsProcessor()

    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action available for marketplace student course with code task`() {
    // when
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    registerEduAiHintsProcessor()
    selectCurrentEduTask(course)

    // but
    HintStateManager.getInstance(project).acceptHint()

    // then
    testGetHintAction(shouldBeEnabled = true, shouldBeVisible = false)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabledAndVisible())
    assertTrue(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action NOT available due to missing processor`() {
    // when only
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    selectCurrentEduTask(course)

    // then
    testGetHintAction(shouldBeEnabled = false, shouldBeVisible = true)
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isEnabled())
  }

  @Test
  fun `GetHint action available`() {
    // when
    acceptAgreement()
    val course = getCourse().apply { isMarketplace = true }
    registerEduAiHintsProcessor()
    selectCurrentEduTask(course)

    // then
    testGetHintAction(shouldBeEnabled = true, shouldBeVisible = true)
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

  private fun registerEduAiHintsProcessor() {
    EduAIHintsProcessor.EP_NAME.addExplicitExtension(PlainTextLanguage.INSTANCE, PlainTextEduAIHintsProcessor(), testRootDisposable)
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
}

private class PlainTextEduAIHintsProcessor : EduAIHintsProcessor {
  override fun getFilesDiffer(): FilesDiffer = object : FilesDiffer {
    override fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean): List<String> = listOf()
  }

  override fun getFunctionDiffReducer(): FunctionDiffReducer = object : FunctionDiffReducer {
    override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement? = null
  }

  override fun getInspectionsProvider(): InspectionsProvider = object : InspectionsProvider {
    override val inspectionIds: Set<String> = emptySet()
  }

  override fun getFunctionSignatureManager(): FunctionSignaturesManager = object : FunctionSignaturesManager {
    override fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> = emptyList()
    override fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? = null
  }

  override fun getStringsExtractor(): StringExtractor = object : StringExtractor {
    override fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings = FunctionsToStrings(emptyMap())
  }
}