package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.lang.Language
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EnvironmentSettingKey
import com.jetbrains.edu.learning.courseFormat.getEnvironmentSetting
import com.jetbrains.edu.learning.courseFormat.setEnvironmentSetting
import javax.swing.Icon

val SQL_TEST_LANGUAGE_KEY = EnvironmentSettingKey("sql_test_language")

var Course.sqlTestLanguage: SqlTestLanguage
  get() {
    val languageId = getEnvironmentSetting(SQL_TEST_LANGUAGE_KEY)
    return SqlTestLanguage.entries.find { it.languageId == languageId } ?: SqlTestLanguage.KOTLIN
  }
  set(value) {
    setEnvironmentSetting(SQL_TEST_LANGUAGE_KEY, value.languageId)
  }

/**
 * Default programming language used for test files
 */
enum class SqlTestLanguage(val languageId: String, val logo: Icon) {
  KOTLIN(EduFormatNames.KOTLIN, EducationalCoreIcons.Language.Kotlin),
  JAVA(EduFormatNames.JAVA, EducationalCoreIcons.Language.Java);

  fun getLanguage(): Language? = Language.findLanguageByID(languageId)
}
