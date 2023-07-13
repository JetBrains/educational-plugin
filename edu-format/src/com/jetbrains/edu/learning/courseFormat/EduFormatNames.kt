package com.jetbrains.edu.learning.courseFormat

object EduFormatNames {
  const val MARKETPLACE = "Marketplace"
  const val MARKETPLACE_YAML_TYPE = "marketplace"
  const val EDU_YAML_TYPE = "edu"

  const val COURSE = "course"
  const val SECTION = "section"
  const val LESSON = "lesson"
  const val FRAMEWORK = "framework"
  const val TASK = "task"
  const val ITEM = "item"

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

  const val DEFAULT_ENVIRONMENT = ""

  // description format
  const val TASK_HTML = "task.html"
  const val TASK_MD = "task.md"

  // Used as course type only
  const val PYCHARM = "PyCharm"

  // Troubleshooting guide links
  const val TROUBLESHOOTING_GUIDE_URL = "https://plugins.jetbrains.com/plugin/10081-jetbrains-academy/docs/troubleshooting-guide.html"
  const val NO_TESTS_URL = "$TROUBLESHOOTING_GUIDE_URL#no_tests_have_run"
  const val FAILED_TO_CHECK_URL = "$TROUBLESHOOTING_GUIDE_URL#failed_to_launch_checking"

  val LOGIN_NEEDED_MESSAGE = message("check.error.login.needed")
}
