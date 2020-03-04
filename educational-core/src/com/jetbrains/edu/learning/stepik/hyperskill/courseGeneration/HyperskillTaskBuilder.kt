package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillLanguages

class HyperskillTaskBuilder(course: Course, lesson: Lesson, stepSource: HyperskillStepSource, stepId: Int)
  : StepikTaskBuilder(course, lesson, stepSource, stepId, -1) {
  override fun getLanguageName(language: Language): String? {
    return HyperskillLanguages.langOfId(language.id).langName
  }
}
