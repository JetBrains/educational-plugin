package com.jetbrains.edu.markdown.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduUriSchemaCompletionProviderTestBase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

class EduMarkdownUriSchemaCompletionProviderTest : EduUriSchemaCompletionProviderTestBase() {
  override val taskDescriptionFormat: DescriptionFormat get() = DescriptionFormat.MD
}
