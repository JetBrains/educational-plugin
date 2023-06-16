package com.jetbrains.edu.learning.yaml.format

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames


@Suppress("UnstableApiUsage")
object YamlMixinNames {

  // common constants
  @NlsSafe
  const val TYPE = JsonMixinNames.TYPE

  @NlsSafe
  const val CONTENT = "content"

  @NlsSafe
  const val CUSTOM_NAME = JsonMixinNames.CUSTOM_NAME

  @NlsSafe
  const val TAGS = JsonMixinNames.TAGS

  // course
  @NlsSafe
  const val TITLE = JsonMixinNames.TITLE

  @NlsSafe
  const val LANGUAGE = JsonMixinNames.LANGUAGE

  @NlsSafe
  const val SUMMARY = JsonMixinNames.SUMMARY

  @NlsSafe
  const val PROGRAMMING_LANGUAGE = "programming_language"

  @NlsSafe
  const val PROGRAMMING_LANGUAGE_VERSION = JsonMixinNames.PROGRAMMING_LANGUAGE_VERSION

  @NlsSafe
  const val SOLUTIONS_HIDDEN = JsonMixinNames.SOLUTIONS_HIDDEN

  @NlsSafe
  const val MODE = "mode"

  @NlsSafe
  const val ENVIRONMENT = JsonMixinNames.ENVIRONMENT

  // coursera course
  @NlsSafe
  const val TOP_LEVEL_LESSONS_SECTION = "default_section"

  @NlsSafe
  const val SUBMIT_MANUALLY = "submit_manually"

  // codeforces contest
  @NlsSafe
  const val END_DATE_TIME = "end_date_time"

  @NlsSafe
  const val PROGRAM_TYPE_ID = "program_type_id"

  //hyperskill course
  @NlsSafe
  const val HYPERSKILL_PROJECT = "hyperskill_project"

  @NlsSafe
  const val STAGES = "stages"

  @NlsSafe
  const val THEORY_ID = "theory_id"

  // marketplace course
  @NlsSafe
  const val VENDOR = JsonMixinNames.VENDOR

  @NlsSafe
  const val IS_PRIVATE = JsonMixinNames.IS_PRIVATE

  @NlsSafe
  const val MARKETPLACE_COURSE_VERSION = JsonMixinNames.MARKETPLACE_COURSE_VERSION

  @NlsSafe
  const val GENERATED_EDU_ID = JsonMixinNames.GENERATED_EDU_ID

  // lesson
  @NlsSafe
  const val UNIT = "unit"

  // framework lesson
  @NlsSafe
  const val CURRENT_TASK = "current_task"

  @NlsSafe
  const val IS_TEMPLATE_BASED = JsonMixinNames.IS_TEMPLATE_BASED

  // task
  @NlsSafe
  const val FILES = JsonMixinNames.FILES

  @NlsSafe
  const val FEEDBACK_LINK = JsonMixinNames.FEEDBACK_LINK

  @NlsSafe
  const val FEEDBACK = "feedback"

  @NlsSafe
  const val STATUS = "status"

  @NlsSafe
  const val RECORD = "record"

  @NlsSafe
  const val SOLUTION_HIDDEN = JsonMixinNames.SOLUTION_HIDDEN

  @NlsSafe
  const val SUBMISSION_LANGUAGE = "submission_language"

  // theory task
  @NlsSafe
  const val POST_SUBMISSION_ON_OPEN = "post_submission_on_open"

  // choice task
  @NlsSafe
  const val IS_CORRECT = "is_correct"

  @NlsSafe
  const val OPTIONS = "options"

  @NlsSafe
  const val ORDERING = "ordering"

  @NlsSafe
  const val CAPTIONS = "captions"

  @NlsSafe
  const val IS_MULTIPLE_CHOICE = "is_multiple_choice"

  @NlsSafe
  const val FEEDBACK_CORRECT = "message_correct"

  @NlsSafe
  const val FEEDBACK_INCORRECT = "message_incorrect"

  @NlsSafe
  const val SELECTED_OPTIONS = "selected_options"
  const val QUIZ_HEADER = "quiz_header"

  const val LOCAL_CHECK = "local_check"

  // checkio missions
  @NlsSafe
  const val CODE = "code"

  @NlsSafe
  const val SECONDS_FROM_CHANGE = "seconds_from_change"

  // codeforces task
  @NlsSafe
  const val PROBLEM_INDEX = "problem_index"

  // codeforces task with file IO
  @NlsSafe
  const val INPUT_FILE = "input_file"

  @NlsSafe
  const val OUTPUT_FILE = "output_file"

  // feedback
  @NlsSafe
  const val MESSAGE = "message"

  @NlsSafe
  const val TIME = "time"

  @NlsSafe
  const val EXPECTED = "expected"

  @NlsSafe
  const val ACTUAL = "actual"

  // task file
  @NlsSafe
  const val NAME = JsonMixinNames.NAME

  @NlsSafe
  const val PLACEHOLDERS = JsonMixinNames.PLACEHOLDERS

  @NlsSafe
  const val VISIBLE = "visible"

  @NlsSafe
  const val LEARNER_CREATED = "learner_created"

  @NlsSafe
  const val TEXT = JsonMixinNames.TEXT

  @NlsSafe
  const val ENCRYPTED_TEXT = "encrypted_text"

  @NlsSafe
  const val EDITABLE = "editable"

  @NlsSafe
  const val HIGHLIGHT_LEVEL = JsonMixinNames.HIGHLIGHT_LEVEL

  // placeholder
  @NlsSafe
  const val OFFSET = JsonMixinNames.OFFSET

  @NlsSafe
  const val LENGTH = JsonMixinNames.LENGTH

  @NlsSafe
  const val PLACEHOLDER_TEXT = JsonMixinNames.PLACEHOLDER_TEXT

  @NlsSafe
  const val DEPENDENCY = JsonMixinNames.DEPENDENCY

  @NlsSafe
  const val INIT_FROM_DEPENDENCY = "initialized_from_dependency"

  @NlsSafe
  const val STUDENT_ANSWER = "student_answer"

  @NlsSafe
  const val INITIAL_STATE = "initial_state"

  @NlsSafe
  const val POSSIBLE_ANSWER = JsonMixinNames.POSSIBLE_ANSWER

  @NlsSafe
  const val ENCRYPTED_POSSIBLE_ANSWER = "encrypted_possible_answer"

  @NlsSafe
  const val SELECTED = "selected"

  // placeholder dependency
  @NlsSafe
  const val SECTION = JsonMixinNames.SECTION

  @NlsSafe
  const val LESSON = JsonMixinNames.LESSON

  @NlsSafe
  const val TASK = JsonMixinNames.TASK

  @NlsSafe
  const val FILE = JsonMixinNames.FILE

  @NlsSafe
  const val PLACEHOLDER = JsonMixinNames.PLACEHOLDER

  @NlsSafe
  const val IS_VISIBLE = JsonMixinNames.IS_VISIBLE

  // remote study item
  @NlsSafe
  const val ID = JsonMixinNames.ID

  @NlsSafe
  const val UPDATE_DATE = JsonMixinNames.UPDATE_DATE
}
