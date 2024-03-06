package com.jetbrains.edu.learning.courseFormat.eduAssistant

/**
 * Represents the possible states of the AI edu assistant.
 * We need to separate these states to be able to update the context if it was generated with poor quality,
 *  e.g., the user requests help several times without changing the code.
 */
enum class AiAssistantState {
  /**
   * A base empty state
   */
  NotInitialized,
  /**
   * This state is set when the context has been automatically initialized, for example, by performing a navigation action.
   */
  ContextInitialized,

  /**
   * This state is set when the context has been initialized by the user request directly
   */
  HelpAsked,
}
