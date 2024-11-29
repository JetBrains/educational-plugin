package com.jetbrains.edu.ai.translation

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.educational.core.format.enum.TranslationLanguage

fun TranslationLanguage?.isSameLanguage(course: EduCourse): Boolean = this?.code == course.languageCode

fun EduCourse?.isSameLanguage(translationLanguage: TranslationLanguage): Boolean = this?.languageCode == translationLanguage.code