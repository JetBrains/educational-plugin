package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.codeInsight.messages.EduCodeInsightBundle
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionLinkProtocol
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionLinkProtocol.*
import org.jetbrains.annotations.Nls

abstract class EduUriSchemaCompletionProviderBase : CompletionProvider<CompletionParameters>() {

  protected abstract fun linkPrefix(parameters: CompletionParameters): String?

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val prefix = linkPrefix(parameters) ?: return
    if (prefix.contains(URLUtil.SCHEME_SEPARATOR)) return

    val lookupElements = completionVariants().map { (protocol, description, scheduleAutoPopup) ->
      val lookupElement = LookupElementBuilder.create(protocol.protocol).withTypeText(description)
      if (scheduleAutoPopup) {
        lookupElement.withInsertHandler { ctx, _ ->
          AutoPopupController.getInstance(ctx.project).scheduleAutoPopup(ctx.editor)
        }
      }
      else {
        lookupElement
      }
    }
    result.withPrefixMatcher(prefix)
      .addAllElements(lookupElements)
  }

  private fun completionVariants(): List<UriSchemaCompletionVariant> {
    return listOf(
      UriSchemaCompletionVariant(PSI_ELEMENT, EduCodeInsightBundle.message("task.description.completion.psi.schema.description"), false),
      UriSchemaCompletionVariant(COURSE, EduCodeInsightBundle.message("task.description.completion.course.schema.description"), true),
      UriSchemaCompletionVariant(SETTINGS, EduCodeInsightBundle.message("task.description.completion.settings.schema.description"), true),
      UriSchemaCompletionVariant(TOOL_WINDOW, EduCodeInsightBundle.message("task.description.completion.tool.window.schema.description"), true)
    )
  }

  private data class UriSchemaCompletionVariant(
    val protocol: TaskDescriptionLinkProtocol,
    val description: @Nls String,
    val scheduleAutoPopup: Boolean
  )
}
