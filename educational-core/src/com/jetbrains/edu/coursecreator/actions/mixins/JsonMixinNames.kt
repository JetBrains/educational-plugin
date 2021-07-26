package com.jetbrains.edu.coursecreator.actions.mixins

object JsonMixinNames {
  // common constants
  const val TYPE = "type"
  const val NAME = "name"
  const val TITLE = "title"
  const val ITEMS = "items"

  // course
  const val AUTHORS = "authors"
  const val LANGUAGE = "language"
  const val SUMMARY = "summary"
  const val PROGRAMMING_LANGUAGE = "programming_language"
  const val COURSE_TYPE = "course_type"
  const val ENVIRONMENT = "environment"
  const val DESCRIPTION_TEXT = "description_text"
  const val DESCRIPTION_FORMAT = "description_format"
  const val ADDITIONAL_FILES = "additional_files"
  const val SOLUTIONS_HIDDEN = "solutions_hidden"
  const val VERSION = "version"
  const val PLUGINS = "plugins"

  //plugin dependency
  const val PLUGIN_NAME = "plugin_name"
  const val PLUGIN_ID = "id"
  const val MAX_VERSION = "max_version"
  const val MIN_VERSION = "min_version"

  // coursera course
  const val SUBMIT_MANUALLY = "submit_manually"

  // lesson
  const val TASK_LIST = "task_list"

  // framework lesson
  const val IS_TEMPLATE_BASED = "is_template_based"

  // task
  const val FILES = "files"
  const val TASK_TYPE = "task_type"
  const val FEEDBACK_LINK = "feedback_link"
  const val CUSTOM_NAME = "custom_name"
  const val SOLUTION_HIDDEN = "solution_hidden"

  // task file
  const val TEXT = "text"
  const val IS_VISIBLE = "is_visible"
  const val PLACEHOLDERS = "placeholders"

  // feedback
  const val LINK = "link"
  const val LINK_TYPE = "link_type"

  // placeholder
  const val OFFSET = "offset"
  const val LENGTH = "length"
  const val PLACEHOLDER_TEXT = "placeholder_text"
  const val POSSIBLE_ANSWER = "possible_answer"
  const val DEPENDENCY = "dependency"

  // remote study item
  const val UPDATE_DATE = "update_date"
  const val ID = "id"
  const val STEPIK_ID = "stepic_id"
  const val UNIT_ID = "unit_id"

  // marketplace
  const val PLUGIN_VERSION = "edu_plugin_version"
  const val VENDOR = "vendor"
  const val IS_PRIVATE = "is_private"
  const val COURSE_VERSION = "course_version"
  const val URL = "url"
  const val EMAIL = "email"
}