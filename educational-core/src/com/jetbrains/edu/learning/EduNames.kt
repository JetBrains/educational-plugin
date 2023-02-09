package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TROUBLESHOOTING_GUIDE_URL

object EduNames {
  const val PLUGIN_ID = "com.jetbrains.edu"
  const val JBA = "Hyperskill"
  const val EDU_PREFIX = "edu"
  const val HINTS = "hints"

  const val FRAMEWORK_LESSON = "framework lesson"
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
  const val CHANGE_NOTES = "change-notes.txt"

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

  // Troubleshooting guide links
  const val NO_COURSES_URL = "$TROUBLESHOOTING_GUIDE_URL#no_courses_found"
  const val CODEFORCES_ANTI_CRAWLER_URL = "$TROUBLESHOOTING_GUIDE_URL#codeforces_anti_crawler"
  const val FAILED_TO_POST_TO_JBA_URL = "$TROUBLESHOOTING_GUIDE_URL#failed_submission_jba"
  const val OUTSIDE_OF_KNOWN_PORT_RANGE_URL = "$TROUBLESHOOTING_GUIDE_URL#outside_of_known_port_range"

  const val ENVIRONMENT_CONFIGURATION_LINK_JAVA = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_java"
  const val ENVIRONMENT_CONFIGURATION_LINK_KOTLIN = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_kotlin"
  const val ENVIRONMENT_CONFIGURATION_LINK_PYTHON = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_python"
  const val ENVIRONMENT_CONFIGURATION_LINK_SCALA = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_scala"
  const val ENVIRONMENT_CONFIGURATION_LINK_JS = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_js"
  const val ENVIRONMENT_CONFIGURATION_LINK_RUST = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_rust"
  const val ENVIRONMENT_CONFIGURATION_LINK_CPP = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_cpp"
  const val ENVIRONMENT_CONFIGURATION_LINK_GO = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_go"
  const val ENVIRONMENT_CONFIGURATION_LINK_PHP = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_php"
  const val ENVIRONMENT_CONFIGURATION_LINK_ANDROID = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_android"
  const val ENVIRONMENT_CONFIGURATION_LINK_GRADLE = "$TROUBLESHOOTING_GUIDE_URL#environment_configuration_gradle"

  // Help links
  const val HELP_URL = "https://www.jetbrains.com/help"
  const val PLUGINS_HELP_LINK = "$HELP_URL/idea/managing-plugins.html"

  const val LEARNER_START_GUIDE = "$HELP_URL/education/learner-start-guide.html"
}