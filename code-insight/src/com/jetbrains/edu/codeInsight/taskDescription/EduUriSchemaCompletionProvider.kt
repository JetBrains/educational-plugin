package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.codeInsight.messages.EduCodeInsightBundle
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionLinkProtocol

class EduUriSchemaCompletionProvider : CompletionProvider<CompletionParameters>() {

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    result.addElement(
      LookupElementBuilder.create(TaskDescriptionLinkProtocol.PSI_ELEMENT.protocol)
        .withTypeText(EduCodeInsightBundle.message("task.description.completion.psi.schema.description"))
    )
    result.addElement(
      LookupElementBuilder.create(TaskDescriptionLinkProtocol.COURSE.protocol)
        .withTypeText(EduCodeInsightBundle.message("task.description.completion.course.schema.description"))
    )
    result.addElement(
      LookupElementBuilder.create(TaskDescriptionLinkProtocol.SETTINGS.protocol)
        .withTypeText(EduCodeInsightBundle.message("task.description.completion.settings.schema.description"))
    )
    result.addElement(
      LookupElementBuilder.create(TaskDescriptionLinkProtocol.TOOL_WINDOW.protocol)
        .withTypeText(EduCodeInsightBundle.message("task.description.completion.tool.window.schema.description"))
    )
  }
}
