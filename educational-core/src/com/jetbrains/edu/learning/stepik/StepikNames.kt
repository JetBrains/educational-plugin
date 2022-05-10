package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.changeHost.StepikHost


object StepikNames {
  const val STEPIK = "Stepik"
  const val STEPIK_TYPE = STEPIK
  const val ARE_SOLUTIONS_UPDATED_PROPERTY = "Educational.StepikSolutionUpdated"
  const val STEPIK_DEFAULT_URL = "https://stepik.org"
  const val STEPIK_HELP = "https://support.stepik.org/hc/en-us/articles/360010601539-Courses-at-Stepik-in-EduTools-JetBrains-plugin-"
  const val STEPIK_HOST_ORDINAL_PROPERTY = "stepik.host.ordinal"
  const val STEPIK_RELEASE_URL = "https://release.stepik.org"

  const val PYCHARM_PREFIX = "pycharm"

  const val PYCHARM_ADDITIONAL = "PyCharm additional materials"
  const val ADDITIONAL_INFO = "additional_files.json"
  const val PLUGIN_NAME = "EduTools"

  @JvmStatic
  fun getStepikUrl(): String = if (isUnitTestMode) {
    STEPIK_RELEASE_URL
  }
  else {
    StepikHost.getSelectedHost().url
  }

  @JvmStatic
  fun getClientId(): String = StepikHost.getSelectedHost().clientId

  @JvmStatic
  fun getClientSecret(): String = StepikHost.getSelectedHost().clientSecret

  @JvmStatic
  fun getStepikApiUrl(): String = "${getStepikUrl()}/api/"

  @JvmStatic
  fun getTokenUrl(): String = "${getStepikUrl()}/oauth2/token/"

  @JvmStatic
  fun getStepikProfilePath(): String = "${getStepikUrl()}/users/"
}