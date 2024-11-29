package com.jetbrains.edu.ai.translation.ui

import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.isSameLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import javax.swing.DefaultComboBoxModel

class TranslationLanguageComboBoxModel(private val course: EduCourse? = null) : DefaultComboBoxModel<TranslationLanguage>() {
  init {
    @OptIn(ExperimentalStdlibApi::class)
    val languages = TranslationLanguage.entries
      .filter { language -> !course.isSameLanguage(language) }
      .sortedBy { it.label }
    addAll(languages)
  }

  override fun setSelectedItem(anObject: Any?) {
    val language = anObject as? TranslationLanguage
    val objectToSelect = if (language == null || course.isSameLanguage(language)) {
      EduAIBundle.message("ai.translation.choose.language")
    }
    else {
      anObject
    }
    super.setSelectedItem(objectToSelect)
  }
}