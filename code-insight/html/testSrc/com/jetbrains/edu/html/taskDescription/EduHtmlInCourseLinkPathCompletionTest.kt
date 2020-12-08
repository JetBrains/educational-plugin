package com.jetbrains.edu.html.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduInCourseLinkPathCompletionTestBase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

class EduHtmlInCourseLinkPathCompletionTest : EduInCourseLinkPathCompletionTestBase() {
  override val taskDescriptionFormat: DescriptionFormat = DescriptionFormat.HTML
}
