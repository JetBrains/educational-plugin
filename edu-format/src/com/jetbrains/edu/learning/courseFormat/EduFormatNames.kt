package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CODEFORCES_TYPE_YAML

object EduFormatNames {
  const val EDU_PREFIX = "edu"
  const val REST_PREFIX = "api"
  const val CODE_ARGUMENT = "code"

  const val MARKETPLACE = "Marketplace"
  const val COURSE_META_FILE = "course.json"
  const val COURSE_ICON_FILE = "courseIcon.svg"

  const val COURSE = "course"
  const val SECTION = "section"
  const val LESSON = "lesson"
  const val FRAMEWORK = "framework"
  const val TASK = "task"
  const val ITEM = "item"


  // marketplace
  const val SOLUTION = "solution"
  const val SUBMISSIONS = "submissions"

  // vendor
  const val EMAIL = "email"
  const val NAME = "name"
  const val URL = "url"

  // Submissions status
  const val CORRECT = "correct"
  const val WRONG = "wrong"
  const val UNCHECKED = "unchecked"

  // attempt
  const val TIME_LEFT = "time_left"
  const val ID = "id"
  const val TIME = "time"
  const val STEP = "step"
  const val DATASET = "dataset"
  const val STATUS = "status"
  const val USER = "user"

  // dataset
  const val IS_MULTIPLE_CHOICE = "is_multiple_choice"
  const val OPTIONS = "options"
  const val PAIRS = "pairs"
  const val ROWS = "rows"
  const val COLUMNS = "columns"
  const val IS_CHECKBOX = "is_checkbox"

  const val DEFAULT_ENVIRONMENT = ""

  // description format
  const val TASK_HTML = "task.html"
  const val TASK_MD = "task.md"

  // Used as course type only
  const val PYCHARM = "PyCharm"

  //hyperskill
  const val TITLE = "title"
  const val THEORY_ID = "theory"
  const val STEP_ID = "step"
  const val IS_COMPLETED = "is_completed"
  const val DESCRIPTION = "description"
  const val IDE_FILES = "ide_files"
  const val USE_IDE = "use_ide"
  const val LANGUAGE = "language"
  const val ENVIRONMENT = "environment"
  const val IS_TEMPLATE_BASED = "is_template_based"
  const val HYPERSKILL_PROBLEMS = "Problems"
  const val HYPERSKILL_TOPICS = "Topics"
  const val TOPICS = "topics"
  const val HYPERSKILL_PROJECTS_URL = "https://hyperskill.org/projects"
  const val HYPERSKILL = "Hyperskill"

  // coursera
  const val COURSERA = "Coursera"

  // checkio
  const val CHECKIO = "CheckiO"

  // codeforces
  const val CODEFORCES: String = "Codeforces"
  const val CODEFORCES_SUBMIT: String = "submit"
  const val CODEFORCES_TASK_TYPE: String = CODEFORCES_TYPE_YAML
  const val CODEFORCES_TASK_TYPE_WITH_FILE_IO: String = "${CODEFORCES_TYPE_YAML}_file_io"
  const val CODEFORCES_URL: String = "https://codeforces.com"

  // stepik
  const val STEPIK = "Stepik"
  const val ATTEMPT = "attempt"
  const val CHECK_PROFILE = "check_profile"

  // IDs of supported languages. They are the same that `Language#getID` returns
  // but in some cases we don't have corresponding Language in classpath to get its id via `getID` method
  const val JAVA = "JAVA"
  const val KOTLIN = "kotlin"
  const val PYTHON = "Python"
  const val SCALA = "Scala"
  const val JAVASCRIPT = "JavaScript"
  const val RUST = "Rust"
  const val SHELL = "Shell Script"
  // Single `ObjectiveC` id is used both for `ObjectiveC` and `C/C++`
  const val CPP = "ObjectiveC"
  const val GO = "go"
  const val PHP = "PHP"
  const val CSHARP = "C#"

  const val PYTHON_2_VERSION = "2.x"
  const val PYTHON_3_VERSION = "3.x"

  // Troubleshooting guide links
  const val TROUBLESHOOTING_GUIDE_URL = "https://plugins.jetbrains.com/plugin/10081-jetbrains-academy/docs/troubleshooting-guide.html"
  const val NO_TESTS_URL = "$TROUBLESHOOTING_GUIDE_URL#no_tests_have_run"
  const val FAILED_TO_CHECK_URL = "$TROUBLESHOOTING_GUIDE_URL#failed_to_launch_checking"

  val LOGIN_NEEDED_MESSAGE = message("check.error.login.needed")
}
