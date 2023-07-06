package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.json.mixins.JsonMixinNames


object YamlMixinNames {

  // common constants
  const val TYPE = JsonMixinNames.TYPE
  const val CONTENT = "content"
  const val CUSTOM_NAME = JsonMixinNames.CUSTOM_NAME
  const val TAGS = JsonMixinNames.TAGS

  // course
  const val TITLE = JsonMixinNames.TITLE
  const val LANGUAGE = JsonMixinNames.LANGUAGE
  const val SUMMARY = JsonMixinNames.SUMMARY
  const val PROGRAMMING_LANGUAGE = "programming_language"
  const val PROGRAMMING_LANGUAGE_VERSION = JsonMixinNames.PROGRAMMING_LANGUAGE_VERSION
  const val SOLUTIONS_HIDDEN = JsonMixinNames.SOLUTIONS_HIDDEN
  const val MODE = "mode"
  const val ENVIRONMENT = JsonMixinNames.ENVIRONMENT

  @NlsSafe
  const val ENVIRONMENT_SETTINGS = JsonMixinNames.ENVIRONMENT_SETTINGS

  // coursera course
  const val TOP_LEVEL_LESSONS_SECTION = "default_section"
  const val SUBMIT_MANUALLY = "submit_manually"

  // codeforces contest
  const val END_DATE_TIME = "end_date_time"
  const val PROGRAM_TYPE_ID = "program_type_id"

  //hyperskill course
  const val HYPERSKILL_PROJECT = "hyperskill_project"
  const val STAGES = "stages"
  const val THEORY_ID = "theory_id"

  // marketplace course
  const val VENDOR = JsonMixinNames.VENDOR
  const val IS_PRIVATE = JsonMixinNames.IS_PRIVATE
  const val MARKETPLACE_COURSE_VERSION = JsonMixinNames.MARKETPLACE_COURSE_VERSION
  const val GENERATED_EDU_ID = JsonMixinNames.GENERATED_EDU_ID

  // lesson
  const val UNIT = "unit"

  // framework lesson
  const val CURRENT_TASK = "current_task"
  const val IS_TEMPLATE_BASED = JsonMixinNames.IS_TEMPLATE_BASED

  // task
  const val FILES = JsonMixinNames.FILES
  const val FEEDBACK_LINK = JsonMixinNames.FEEDBACK_LINK
  const val FEEDBACK = "feedback"
  const val STATUS = "status"
  const val RECORD = "record"
  const val SOLUTION_HIDDEN = JsonMixinNames.SOLUTION_HIDDEN
  const val SUBMISSION_LANGUAGE = "submission_language"

  // theory task
  const val POST_SUBMISSION_ON_OPEN = "post_submission_on_open"

  // choice task
  const val IS_CORRECT = "is_correct"
  const val OPTIONS = "options"
  const val ORDERING = "ordering"
  const val CAPTIONS = "captions"
  const val IS_MULTIPLE_CHOICE = "is_multiple_choice"
  const val FEEDBACK_CORRECT = "message_correct"
  const val FEEDBACK_INCORRECT = "message_incorrect"
  const val SELECTED_OPTIONS = "selected_options"
  const val QUIZ_HEADER = "quiz_header"
  const val LOCAL_CHECK = "local_check"

  // checkio missions
  const val CODE = "code"
  const val SECONDS_FROM_CHANGE = "seconds_from_change"

  // codeforces task
  const val PROBLEM_INDEX = "problem_index"

  // codeforces task with file IO
  const val INPUT_FILE = "input_file"
  const val OUTPUT_FILE = "output_file"

  // feedback
  const val MESSAGE = "message"
  const val TIME = "time"
  const val EXPECTED = "expected"
  const val ACTUAL = "actual"

  // task file
  const val NAME = JsonMixinNames.NAME
  const val PLACEHOLDERS = JsonMixinNames.PLACEHOLDERS
  const val VISIBLE = "visible"
  const val LEARNER_CREATED = "learner_created"
  const val TEXT = JsonMixinNames.TEXT
  const val ENCRYPTED_TEXT = "encrypted_text"
  const val EDITABLE = "editable"
  const val HIGHLIGHT_LEVEL = JsonMixinNames.HIGHLIGHT_LEVEL

  // placeholder
  const val OFFSET = JsonMixinNames.OFFSET
  const val LENGTH = JsonMixinNames.LENGTH
  const val PLACEHOLDER_TEXT = JsonMixinNames.PLACEHOLDER_TEXT
  const val DEPENDENCY = JsonMixinNames.DEPENDENCY
  const val INIT_FROM_DEPENDENCY = "initialized_from_dependency"
  const val STUDENT_ANSWER = "student_answer"
  const val INITIAL_STATE = "initial_state"
  const val POSSIBLE_ANSWER = JsonMixinNames.POSSIBLE_ANSWER
  const val ENCRYPTED_POSSIBLE_ANSWER = "encrypted_possible_answer"
  const val SELECTED = "selected"

  // placeholder dependency
  const val SECTION = JsonMixinNames.SECTION
  const val LESSON = JsonMixinNames.LESSON
  const val TASK = JsonMixinNames.TASK
  const val FILE = JsonMixinNames.FILE
  const val PLACEHOLDER = JsonMixinNames.PLACEHOLDER
  const val IS_VISIBLE = JsonMixinNames.IS_VISIBLE

  // remote study item
  const val ID = JsonMixinNames.ID
  const val UPDATE_DATE = JsonMixinNames.UPDATE_DATE
}
