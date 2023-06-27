package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import org.apache.commons.lang3.LocaleUtils
import java.util.*

private const val HUMAN_LANGUAGE = "humanLanguage"
private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
private const val PROGRAMMING_LANGUAGE_ID = "programmingLanguageId"
private const val PROGRAMMING_LANGUAGE_VERSION = "programmingLanguageVersion"
@Tag(EduNames.COURSE)
class CourseMetaInfo() : Course() {
  var type: String = ""
  var location: String = ""
  var tasksTotal: Int = 0
  var tasksSolved: Int = 0

  override var parent: ItemContainer
    @Transient
    get() = super.parent
    @Transient
    set(_) {
    }

  constructor(location: String = "", course: Course, tasksTotal: Int = 0, tasksSolved: Int = 0) : this() {
    this.type = course.itemType
    id = course.id
    name = course.name
    description = course.description
    courseMode = course.courseMode
    environment = course.environment
    languageId = course.languageId
    languageVersion = course.languageVersion
    languageCode = course.languageCode
    this.location = location
    this.tasksTotal = tasksTotal
    this.tasksSolved = tasksSolved
  }

  override val itemType
    @Transient
    get() = type

  /**
   * Used only for migration, see EDU-5856
   */
  @Suppress("MemberVisibilityCanBePrivate", "unused")
  var oldProgrammingLanguage: String? = null
    @OptionTag(PROGRAMMING_LANGUAGE)
    set(value) {
      if (value == null) return
      convertProgrammingLanguageVersion(value)
      field = null
    }

  override var languageId: String
    @OptionTag(PROGRAMMING_LANGUAGE_ID)
    get() = super.languageId
    @OptionTag(PROGRAMMING_LANGUAGE_ID)
    set(value) {
      if (value.isEmpty()) return
      super.languageId = value
    }

  override var languageVersion: String?
    @OptionTag(PROGRAMMING_LANGUAGE_VERSION)
    get() = super.languageVersion
    @OptionTag(PROGRAMMING_LANGUAGE_VERSION)
    set(value) {
      if (value == null) return
      super.languageVersion = value
    }

  override val humanLanguage: String
    get() {
      try {
        val locale = Locale.Builder().setLanguageTag(languageCode).build()
        if (languageCode.length > 3 && !LocaleUtils.isAvailableLocale(locale)) {
          convertLanguageCode()
        }
      }
      catch (e: IllformedLocaleException) {
        convertLanguageCode()
      }
      return super.humanLanguage
    }

  @OptionTag(HUMAN_LANGUAGE)
  override var languageCode = super.languageCode

  private fun convertLanguageCode() {
    val languageCode = Locale.getAvailableLocales().find { it.displayName == this.languageCode }?.toLanguageTag()
    if (languageCode != null) {
      this.languageCode = languageCode
    }
    else {
      Logger.getInstance(this::class.java).warn("Cannot find locale for '${super.languageCode}'")
    }
  }

  private fun convertProgrammingLanguageVersion(value: String) {
    value.split(" ").apply {
      super.languageId = first()
      super.languageVersion = getOrNull(1)
    }
  }

  fun toCourse(): Course {
    return this
  }
}