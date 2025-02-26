package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.changeHost.StepikHost


object StepikNames {
  const val STEPIK = "Stepik"
  const val STEPIK_DEFAULT_URL = "https://stepik.org"
  const val STEPIK_HOST_ORDINAL_PROPERTY = "stepik.host.ordinal"
  const val COGNITERRA_URL = "https://cogniterra.org"

  const val PYCHARM_PREFIX = "pycharm"

  const val PYCHARM_ADDITIONAL = "PyCharm additional materials"
  const val ADDITIONAL_INFO = "additional_files.json"
  const val PLUGIN_NAME = "EduTools"

  fun getStepikUrl(): String = if (isUnitTestMode) {
    "https://testURL.org"
  }
  else {
    StepikHost.getSelectedHost().url
  }

  fun getClientId(): String = StepikHost.getSelectedHost().clientId

  fun getTokenUrl(): String = "${getStepikUrl()}/oauth2/token/"

  fun getStepikProfilePath(): String = "${getStepikUrl()}/users/"
}