package com.jetbrains.edu.coursecreator.testGeneration.request

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.testGeneration.request.PromptUtil.generatePrompt
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiClassWrapper
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiHelper
import com.jetbrains.edu.coursecreator.testGeneration.util.SettingsArguments
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.generation.llm.prompt.PromptSizeReductionStrategy

class PromptSizeReductionDefaultStrategy(private val project: Project, private val testGenerationData: TestGenerationData, private val psiHelper: PsiHelper, val classesToTest: List<PsiClassWrapper>) : PromptSizeReductionStrategy {
  override fun isReductionPossible(): Boolean = (SettingsArguments(project).maxPolyDepth(testGenerationData.polyDepthReducing) > 1) ||
                                                (SettingsArguments(project).maxInputParamsDepth(testGenerationData.inputParamsDepthReducing) > 1)

  private fun reducePromptSize(): Boolean {
    if (SettingsArguments(project).maxPolyDepth(testGenerationData.polyDepthReducing) > 1) {
      testGenerationData.polyDepthReducing++
      return true
    }

    // reducing depth of input params
    if (SettingsArguments(project).maxInputParamsDepth(testGenerationData.inputParamsDepthReducing) > 1) {
      testGenerationData.inputParamsDepthReducing++
      return true
    }
    return false
  }

  override fun reduceSizeAndGeneratePrompt(): String {

    if (!isReductionPossible()) {
      throw IllegalStateException("Prompt size reduction is not possible yet requested")
    }

    val reductionSuccess = reducePromptSize()
    assert(reductionSuccess)

    return generatePrompt(project, psiHelper, 0, classesToTest)
  }
}
