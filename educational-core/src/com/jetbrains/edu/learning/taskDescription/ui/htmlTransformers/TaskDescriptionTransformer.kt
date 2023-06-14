package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers

import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps.*

private val TaskDescriptionHtmlTransformer = HtmlTransformer.pipeline(
  MediaThemesTransformer,
  ExternalLinkIconsTransformer,
  CodeHighlighter,
  HintsWrapper
)

val TaskDescriptionTransformer = StringHtmlTransformer.pipeline(
  TaskDescriptionHtmlTransformer.toStringTransformer(),
  ResourceWrapper
)