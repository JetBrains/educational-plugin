package com.jetbrains.edu.learning

object EduNames {
  const val PLUGIN_ID = "com.jetbrains.edu"
  const val JBA = "JetBrains Academy"
  const val EDU_PREFIX = "edu"
  const val TASK_HTML = "task.html"
  const val TASK_MD = "task.md"
  const val HINTS = "hints"
  const val LESSON = "lesson"
  const val FRAMEWORK_LESSON = "framework lesson"
  const val FRAMEWORK = "framework"
  const val TASK = "task"
  const val COURSE = "course"
  const val ITEM = "item"
  const val PROJECT = "project"
  const val WINDOW_POSTFIX = "_window."
  const val WINDOWS_POSTFIX = "_windows"
  const val ANSWERS_POSTFIX = "_answers"
  const val TEST_HELPER = "test_helper.py"
  const val COURSE_META_FILE = "course.json"
  const val ADDITIONAL_MATERIALS = "Edu additional materials"
  const val PROJECT_NAME = "PROJECT_NAME"
  const val RUN_CONFIGURATION_DIR = "runConfigurations"
  const val DEFAULT_ENVIRONMENT = ""

  // Used as course type only
  const val PYCHARM = "PyCharm"
  const val EDU = "Edu"
  const val ANDROID = "Android"
  const val UNITTEST = "unittest"
  const val SRC = "src"
  const val TEST = "test"
  const val BUILD = "build"
  const val OUT = "out"
  const val STUDY_PROJECT_XML_PATH = "/.idea/study_project.xml"
  const val STEPIK_IDS_JSON = "stepik_ids.json"
  const val COURSE_IGNORE = ".courseignore"

  // IDs of supported languages. They are the same that `Language#getID` returns
  // but in some cases we don't have corresponding Language in classpath to get its id via `getID` method
  const val JAVA = "JAVA"
  const val KOTLIN = "kotlin"
  const val PYTHON = "Python"
  const val SCALA = "Scala"
  const val JAVASCRIPT = "JavaScript"
  const val RUST = "Rust"

  // Single `ObjectiveC` id is used both for `ObjectiveC` and `C/C++`
  const val CPP = "ObjectiveC"
  const val GO = "go"
  const val PHP = "PHP"

  // Language versions
  const val PYTHON_2_VERSION = "2.x"
  const val PYTHON_3_VERSION = "3.x"

  // Submissions status
  const val CORRECT = "correct"
  const val WRONG = "wrong"
  const val UNCHECKED = "unchecked"

  // Troubleshooting guide links
  private const val TROUBLESHOOTING_GUIDE_URL = "https://plugins.jetbrains.com/plugin/10081-edutools/docs/troubleshooting-guide.html"
  const val NO_TESTS_URL = "$TROUBLESHOOTING_GUIDE_URL#no_tests_have_run"
  const val NO_COURSES_URL = "$TROUBLESHOOTING_GUIDE_URL#no_courses_found"
  const val FAILED_TO_POST_TO_JBA_URL = "$TROUBLESHOOTING_GUIDE_URL#failed_submission_jba"
  const val FAILED_TO_CHECK_URL = "$TROUBLESHOOTING_GUIDE_URL#failed_to_launch_checking"
  const val OUTSIDE_OF_KNOWN_PORT_RANGE_URL = "$TROUBLESHOOTING_GUIDE_URL#outside_of_known_port_range"

  // Help links
  const val HELP_URL = "https://www.jetbrains.com/help"
  const val LEARNER_START_GUIDE = "$HELP_URL/education/learner-start-guide.html"
}