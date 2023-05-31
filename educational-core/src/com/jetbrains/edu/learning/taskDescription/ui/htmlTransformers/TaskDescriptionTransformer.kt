package com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers

import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.steps.*

private val TaskDescriptionTransformerStep1 = pipeline(
  MediaThemesTransformer,
  ExternalLinkIconsTransformer,
  CodeHighlighter,
  HintsWrapper
)

val TaskDescriptionTransformer = pipeline(
  TaskDescriptionTransformerStep1.toStringTransformer(),
  ResourceWrapper
)