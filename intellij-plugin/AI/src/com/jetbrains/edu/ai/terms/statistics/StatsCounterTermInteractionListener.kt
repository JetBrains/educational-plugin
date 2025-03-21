package com.jetbrains.edu.ai.terms.statistics

import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.learning.ai.terms.TermsInteractionListener
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Tracks and processes events where terms are hovered over or viewed.
 *
 * TODO(remove this class after moving leftover logic from `educational-core` to separate package)
 */
class StatsCounterTermInteractionListener : TermsInteractionListener {
  override fun termHovered(task: Task) {
    EduAIFeaturesCounterUsageCollector.theoryLookupTermHovered(task)
  }

  override fun termViewed(task: Task) {
    EduAIFeaturesCounterUsageCollector.theoryLookupTermViewed(task)
  }
}