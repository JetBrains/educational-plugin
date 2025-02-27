package com.jetbrains.edu.decomposition.feedback

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.jetbrains.edu.decomposition.model.FunctionModel

interface GranularityFeedbackProvider {
  fun provideFeedback(project: Project, functionModels: List<FunctionModel>)

  companion object {
    private val EP_NAME = LanguageExtension<GranularityFeedbackProvider>("Educational.granularityFeedbackProvider")

    fun provideFeedback(functionModels: List<FunctionModel>, project: Project, language: Language) {
      EP_NAME.forLanguage(language)?.provideFeedback(project, functionModels)
    }
  }
}