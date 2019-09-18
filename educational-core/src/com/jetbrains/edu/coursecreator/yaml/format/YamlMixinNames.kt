package com.jetbrains.edu.coursecreator.yaml.format

object YamlMixinNames {

  // common constants
  const val TYPE = "type"
  const val CONTENT = "content"
  const val CUSTOM_NAME = "custom_name"

  // course
  const val TITLE = "title"
  const val LANGUAGE = "language"
  const val SUMMARY = "summary"
  const val PROGRAMMING_LANGUAGE = "programming_language"
  const val SOLUTIONS_HIDDEN = "solutions_hidden"

  const val ENVIRONMENT = "environment"
  const val PROGRAMMING_LANGUAGE_VERSION = "programming_language_version"

  // coursera course
  const val TOP_LEVEL_LESSONS_SECTION = "default_section"
  const val SUBMIT_MANUALLY = "submit_manually"

  // lesson
  const val UNIT = "unit"

  // task
  const val FILES = "files"
  const val FEEDBACK_LINK = "feedback_link"
  const val STATUS = "status"
  const val RECORD = "record"

  // choice task
  const val IS_CORRECT = "is_correct"
  const val OPTIONS = "options"
  const val IS_MULTIPLE_CHOICE = "is_multiple_choice"
  const val FEEDBACK_CORRECT = "message_correct"
  const val FEEDBACK_INCORRECT = "message_incorrect"

  // task file
  const val NAME = "name"
  const val PLACEHOLDERS = "placeholders"
  const val VISIBLE = "visible"

  // placeholder
  const val OFFSET = "offset"
  const val LENGTH = "length"
  const val PLACEHOLDER_TEXT = "placeholder_text"
  const val DEPENDENCY = "dependency"
  
  // placeholder dependency
  const val SECTION = "section"
  const val LESSON = "lesson"
  const val TASK = "task"
  const val FILE = "file"
  const val PLACEHOLDER = "placeholder"
  const val IS_VISIBLE = "is_visible"

  // remote study item
  const val ID = "id"
  const val UPDATE_DATE = "update_date"
}
