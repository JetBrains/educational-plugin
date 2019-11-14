package com.jetbrains.edu.learning.yaml.format

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
  const val MODE = "mode"

  const val ENVIRONMENT = "environment"
  const val PROGRAMMING_LANGUAGE_VERSION = "programming_language_version"

  // coursera course
  const val TOP_LEVEL_LESSONS_SECTION = "default_section"
  const val SUBMIT_MANUALLY = "submit_manually"

  //hyperskill course
  const val HYPERSKILL_PROJECT = "hyperskill_project"

  // lesson
  const val UNIT = "unit"

  // framework lesson
  const val CURRENT_TASK = "current_task"

  // task
  const val FILES = "files"
  const val FEEDBACK_LINK = "feedback_link"
  const val STATUS = "status"
  const val RECORD = "record"
  const val SOLUTION_HIDDEN = "solution_hidden"

  // choice task
  const val IS_CORRECT = "is_correct"
  const val OPTIONS = "options"
  const val IS_MULTIPLE_CHOICE = "is_multiple_choice"
  const val FEEDBACK_CORRECT = "message_correct"
  const val FEEDBACK_INCORRECT = "message_incorrect"
  const val SELECTED_OPTIONS = "selected_options"

  // video task
  const val THUMBNAIL = "thumbnail"
  const val SOURCES = "sources"
  const val CURRENT_TIME = "currentTime"
  const val SRC = "src"
  const val RES = "res"
  const val VIDEO_TYPE = "type"
  const val LABEL = "label"

  // checkio missions
  const val CODE = "code"
  const val SECONDS_FROM_CHANGE = "seconds_from_change"

  // task file
  const val NAME = "name"
  const val PLACEHOLDERS = "placeholders"
  const val VISIBLE = "visible"
  const val LEARNER_CREATED = "learner_created"
  const val TEXT = "text"

  // placeholder
  const val OFFSET = "offset"
  const val LENGTH = "length"
  const val PLACEHOLDER_TEXT = "placeholder_text"
  const val DEPENDENCY = "dependency"
  const val INIT_FROM_DEPENDENCY = "initialized_from_dependency"
  const val STUDENT_ANSWER = "student_answer"
  const val INITIAL_STATE = "initial_state"
  const val POSSIBLE_ANSWER = "possible_answer"
  const val SELECTED = "selected"

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
