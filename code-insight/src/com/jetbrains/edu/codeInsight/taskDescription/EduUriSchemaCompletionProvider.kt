package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.codeInsight.messages.EduCodeInsightBundle
import com.jetbrains.edu.learning.taskDescription.ui.ToolWindowLinkHandler

class EduUriSchemaCompletionProvider : CompletionProvider<CompletionParameters>() {

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    result.addElement(LookupElementBuilder.create(ToolWindowLinkHandler.PSI_ELEMENT_PROTOCOL)
                        .withTypeText(EduCodeInsightBundle.message("task.description.completion.psi.schema.description")))

    result.addElement(LookupElementBuilder.create(ToolWindowLinkHandler.IN_COURSE_PROTOCOL)
                        .withTypeText(EduCodeInsightBundle.message("task.description.completion.course.schema.description")))
  }
}
