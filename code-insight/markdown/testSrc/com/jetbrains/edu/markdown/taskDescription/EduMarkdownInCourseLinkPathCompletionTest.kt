package com.jetbrains.edu.markdown.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduInCourseLinkPathCompletionTestBase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

class EduMarkdownInCourseLinkPathCompletionTest : EduInCourseLinkPathCompletionTestBase() {
  override val taskDescriptionFormat: DescriptionFormat = DescriptionFormat.MD
}
