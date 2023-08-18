package com.jetbrains.edu.learning.checkio.utils

import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.api.exceptions.HttpException
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOCourse
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorState
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorState.CustomSevereError

object CheckiOCourseGenerationUtils {
  @Throws(CourseCantBeStartedException::class)
  fun getCourseFromServerUnderProgress(
    contentGenerator: CheckiOCourseContentGenerator,
    course: CheckiOCourse,
    account: CheckiOAccount?,
    link: String
  ) {
    try {
      val stations = contentGenerator.getStationsFromServerUnderProgress()
      stations.forEach { course.addStation(it) }
    }
    catch (e: Exception) {
      throw CourseCantBeStartedException(getErrorState(e, account, link))
    }
  }

  private fun getErrorState(e: Exception, account: CheckiOAccount?, link: String): ErrorState {
    if (e is HttpException && e.response.code() == 401 && account != null) {
      return CustomSevereError(
        message("validation.open.checkio.to.verify.account")
      ) { EduBrowser.getInstance().browse("$link/login/checkio/") }
    }
    else if (e is NetworkException) {
      return CustomSevereError(message("error.failed.to.connect"), null)
    }
    return CustomSevereError(e.message ?: "No error message", null)
  }
}
