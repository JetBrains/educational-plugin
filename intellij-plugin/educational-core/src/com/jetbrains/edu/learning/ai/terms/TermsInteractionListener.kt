package com.jetbrains.edu.learning.ai.terms

import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Listener interface for handling user interactions with terms, such as hovering over a term or viewing its tooltip.
 *
 * TODO(remove this class after moving leftover logic from `educational-core` to separate package)
 */
interface TermsInteractionListener {
  /**
   * Triggered when the user hovers over a term.
   */
  fun termHovered(task: Task)

  /**
   * Triggered when the user keeps the term tooltip open for more than 2 seconds.
   */
  fun termViewed(task: Task)

  companion object {
    private const val TOPIC_NAME = "TERM_INTERACTION_TOPIC"

    val TOPIC = Topic<TermsInteractionListener>.create(TOPIC_NAME, TermsInteractionListener::class.java)
  }
}