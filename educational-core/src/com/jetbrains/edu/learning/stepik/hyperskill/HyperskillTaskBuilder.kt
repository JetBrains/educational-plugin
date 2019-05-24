package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder

class HyperskillTaskBuilder(language: Language,
                            lesson: Lesson,
                            stepSource: HyperskillStepSource,
                            stepId: Int) : StepikTaskBuilder(language, lesson, stepSource, stepId, -1) {
  override fun getLanguageName(language: Language): String? {
    return HyperskillLanguages.langOfId(language.id).langName
  }
}
