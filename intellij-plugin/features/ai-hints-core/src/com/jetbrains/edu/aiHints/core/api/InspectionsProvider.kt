package com.jetbrains.edu.aiHints.core.api

interface InspectionsProvider {
  /**
   * A set of inspections that will be applied to the [com.jetbrains.educational.ml.hints.hint.CodeHint].
   *
   * @see [com.jetbrains.edu.aiHints.core.TaskProcessorImpl.applyInspections]
   */
  val inspectionIds: Set<String>
}