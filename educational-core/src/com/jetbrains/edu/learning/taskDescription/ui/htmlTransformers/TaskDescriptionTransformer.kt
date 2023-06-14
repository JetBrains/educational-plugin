package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers

import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps.*

private val TaskDescriptionHtmlTransformer = pipeline(
  MediaThemesTransformer,
  ExternalLinkIconsTransformer,
  CodeHighlighter,
  HintsWrapper
)

val TaskDescriptionTransformer = pipeline(
  TaskDescriptionHtmlTransformer.toStringTransformer(),
  ResourceWrapper
)