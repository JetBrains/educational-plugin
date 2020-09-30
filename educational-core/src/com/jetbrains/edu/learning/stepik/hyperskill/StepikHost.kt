package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikOAuthBundle.valueOrDefault

enum class StepikHost(val url: String, val clientId: String, val clientSecret: String) {
  PRODUCTION(StepikNames.STEPIK_DEFAULT_URL, valueOrDefault("stepikClientId", ""), valueOrDefault("stepikClientSecret", "")),
  RELEASE(StepikNames.STEPIK_RELEASE_URL, "CdBEN1zAyAO8FTgNQcOIKYENNDLSawKfx3Uqe71Z",
          "hrcgb6oirjprvnCG6MHAesjRfX0v6u7ci0rcuMxGAKFxfue3G68Zyn6shY4BIHaMP7bdVkWx9lypBWF1vzRVJBMepIv0pB4A4KqiTiM4did7hCaCdNU6x9N0nML2ouNp"),
  DEV(StepikNames.STEPIK_RELEASE_URL, "CdBEN1zAyAO8FTgNQcOIKYENNDLSawKfx3Uqe71Z",
      "hrcgb6oirjprvnCG6MHAesjRfX0v6u7ci0rcuMxGAKFxfue3G68Zyn6shY4BIHaMP7bdVkWx9lypBWF1vzRVJBMepIv0pB4A4KqiTiM4did7hCaCdNU6x9N0nML2ouNp");

  override fun toString(): String {
    return url
  }

  companion object {
    fun getSelectedHost(): StepikHost? = values().firstOrNull {
      it.url == PropertiesComponent.getInstance().getValue(StepikNames.STEPIK_URL_PROPERTY, StepikNames.STEPIK_URL)
    }
  }
}