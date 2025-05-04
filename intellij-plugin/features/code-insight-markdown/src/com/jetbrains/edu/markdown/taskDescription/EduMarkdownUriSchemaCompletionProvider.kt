package com.jetbrains.edu.markdown.taskDescription

import com.intellij.codeInsight.completion.CompletionParameters
import com.jetbrains.edu.codeInsight.taskDescription.EduUriSchemaCompletionProviderBase
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestination

class EduMarkdownUriSchemaCompletionProvider : EduUriSchemaCompletionProviderBase() {
  override fun linkPrefix(parameters: CompletionParameters): String? {
    val linkDestination = parameters.position.parent as? MarkdownLinkDestination ?: return null
    // In the case of a custom URI scheme, markdown link destination has three children in PSI tree.
    // Since we want to provide completion regarding what exact child is currently under the caret,
    // let's just extract text of whole link destination as a prefix
    return linkDestination.text.substring(0, parameters.offset - linkDestination.textRange.startOffset)
  }
}
