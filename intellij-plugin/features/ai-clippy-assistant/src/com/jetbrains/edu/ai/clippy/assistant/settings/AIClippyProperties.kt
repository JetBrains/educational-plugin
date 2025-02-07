package com.jetbrains.edu.ai.clippy.assistant.settings

import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.ml.core.context.Context

data class AIClippyProperties(
  val language: TranslationLanguage,
  /**
   * Configures the level of aggression in the assistant's responses.
   * The scale ranges from 1 (gentle and kind) to 10 (highly intense, assertive, or forceful feedback).
   * Lower values encourage a calm and supportive tone, while higher values allow for more confrontational or intense motivational feedback.
   */
  val aggression: Int,
  /**
   * Configures the assistant's communication style.
   * The scale ranges from 1 (formal, professional language) to 10 (casual, friendly, and personal tone).
   * A lower value keeps the tone serious and professional, whereas a higher value results in warm, approachable, and engaging language.
   */
  val communicationStyle: Int,
  /**
   * Configures the assistant's use of emojis in its feedback.
   * The scale ranges from 1 (no emoji usage) to 10 (frequent emoji usage in responses).
   * A lower value results in plain text communication without any visual elements, suitable for formal or professional contexts.
   * A higher value incorporates emojis extensively to add expressiveness, enthusiasm, or fun, making the feedback more engaging and lively.
   */
  val emojiUsage: Int,
  /**
   * Controls the emotional intensity of the assistant's feedback.
   * The scale ranges from 1 (neutral and professional tone) to 10 (highly emotional and enthusiastic responses).
   * Lower values focus on reserved, unemotional messages, whereas higher values
   * bring lively and reinforcing messages like "Well done!" or "Amazing!"
   */
  val emotionalIntensity: Int,
  /**
   * Configures the level of humiliating tone in the assistant's response.
   * The scale ranges from 1 (none, supportive communication) to 10 (highly humiliating and sarcastic tone).
   * Lower values emphasize encouragement, while higher values allow strong and harsh criticism, often including sarcasm or humiliation.
   */
  val humiliation: Int,
  /**
   * Defines the assistant's focus between highlighting mistakes or achievements.
   * The scale ranges from 1 (greater focus on mistakes with constructive feedback)
   * to 10 (greater focus on achievements, with errors receiving less attention).
   * A lower value encourages the user to improve by pointing out errors, and
   * a higher value celebrates successes while minimizing criticism.
   */
  val mistakesAttention: Int,
) : Context {
  constructor() : this(TranslationLanguage.ENGLISH, 5, 5, 5, 5, 5, 5)

  init {
    require(aggression in 1..10) {
      "Invalid value for aggression: $aggression. Aggression must be between 11 (gentle and kind) to 10 (highly intense, assertive, or forceful feedback)."
    }
    require(communicationStyle in 1..10) {
      "Invalid value for communicationStyle: $communicationStyle. Style must be between 1 (formal) and 10 (friendly)."
    }
    require(emojiUsage in 1..10) {
      "Invalid value for emojiUsage: $emojiUsage. Usage must be between 1 (no emoji usage) and 10 (frequent emoji usage in responses)."
    }
    require(emotionalIntensity in 1..10) {
      "Invalid value for emotionalIntensity: $emotionalIntensity. Intensity must be between 1 (neutral) and 10 (highly emotional)."
    }
    require(humiliation in 1..10) {
      "Invalid value for humiliation: $humiliation. Humiliation must be between 1 (none) and 10 (highly humiliating)."
    }
    require(mistakesAttention in 1..10) {
      "Invalid value for mistakesAttention: $mistakesAttention. Focus must be between 1 (mistakes) and 10 (achievements)."
    }
  }
}