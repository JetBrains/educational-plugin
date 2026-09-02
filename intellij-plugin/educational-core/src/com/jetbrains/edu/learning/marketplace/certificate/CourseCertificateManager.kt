package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.CourseCertificateIssueResponse
import com.jetbrains.edu.learning.marketplace.api.CourseCertificationSupportResponse
import com.jetbrains.edu.learning.marketplace.api.LearningCenterConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.projectView.ProgressUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps track of whether the current course provides a certificate and issues it as soon as the user
 * has completed enough of the course.
 *
 * The state is requested from the Learning Center lazily (see [updateCertificateState]) and is kept in memory only,
 * so it's requested anew in every IDE session.
 */
@Service(Service.Level.PROJECT)
class CourseCertificateManager(private val project: Project, val scope: CoroutineScope) : EduTestAware {

  private val mutex = Mutex()

  @Volatile
  var state: CourseCertificationState = CourseCertificationState.Unknown
    private set

  /**
   * Whether a certificate can be obtained for the current course, i.e. its state is
   * [CourseCertificationState.Supported] or [CourseCertificationState.Issued].
   *
   * Unlike [updateCertificateState], it never issues a certificate — at most one
   * `courseCertificationSupport` request is sent, and only while the state is still
   * [CourseCertificationState.Unknown]. Meant for callers that only need to know whether the course
   * provides a certificate at all, for example, to suppress other achievement dialogs.
   */
  suspend fun hasCertification(): Boolean = mutex.withLock {
    if (state is CourseCertificationState.Unknown) {
      val course = certifiableCourse()
      if (course == null) {
        state = CourseCertificationState.Unsupported
        return@withLock state.hasCertification
      }
      loadCertificationSupport(course)
    }
    state.hasCertification
  }

  /**
   * Refreshes and returns the certification state of the current course, see [state].
   *
   * Nothing is requested if a certificate can't be issued for the course at all (see [certifiableCourse])
   * or if the state is already final. Otherwise, for a logged-in user a single `tryIssueCertificate`
   * request both reports the completed percent and returns the certificate once the required progress
   * is reached; for an anonymous user only the required progress is loaded, since a certificate can't
   * be issued without an account.
   *
   * Should be called whenever the progress state may have changed, for example, after a task is solved
   */
  suspend fun updateCertificateState(): CourseCertificationState = mutex.withLock {
    val course = certifiableCourse()
    if (course == null) {
      state = CourseCertificationState.Unsupported
      return@withLock state
    }
    if (state.isFinal) return@withLock state

    updateCertificateState(course)
    state
  }

  /**
   * Must be called under [mutex]
   */
  private suspend fun updateCertificateState(course: EduCourse) {
    val isLoggedIn = MarketplaceConnector.getInstance().isLoggedIn()
    when (state) {
      is CourseCertificationState.Supported -> {
        if (isEligibleForCertificate(course) && isLoggedIn) {
          issueCertificate(course)
        }
      }

      else -> {
        if (isLoggedIn) {
          // The `issue` endpoint reports the required progress as well, so a single request is enough for a logged-in user
          issueCertificate(course)
        }
        else {
          loadCertificationSupport(course)
        }
      }
    }
  }

  /**
   * Must be called under [mutex]
   */
  private suspend fun loadCertificationSupport(course: EduCourse) {
    val response = LearningCenterConnector.getInstance().courseCertificationSupport(course.id).onError {
      LOG.warn("Failed to load certification support for course ${course.id}: $it")
      return
    }
    state = when (response) {
      is CourseCertificationSupportResponse.Supported -> CourseCertificationState.Supported(response.requiredPercent)
      CourseCertificationSupportResponse.Unsupported -> CourseCertificationState.Unsupported
    }
  }

  /**
   * Must be called under [mutex]
   */
  private suspend fun issueCertificate(course: EduCourse) {
    val response = LearningCenterConnector.getInstance().tryIssueCertificate(completedPercent(course), course.id).onError {
      LOG.warn("Failed to issue a certificate for course ${course.id}: $it")
      return
    }
    when (response) {
      is CourseCertificateIssueResponse.Issued -> {
        state = CourseCertificationState.Issued(response.certificateId, response.isFirstClientRequest)
      }

      is CourseCertificateIssueResponse.NotReady -> {
        state = CourseCertificationState.Supported(response.requiredPercent)
      }

      CourseCertificateIssueResponse.Unsupported -> {
        state = CourseCertificationState.Unsupported
      }
    }
  }

  fun isEligibleForCertificate(course: Course): Boolean {
    val currentState = state
    return currentState is CourseCertificationState.Supported && completedPercent(course) >= currentState.requiredPercent
  }

  /**
   * Returns the current course if a certificate can be issued for it, and `null` otherwise
   */
  private fun certifiableCourse(): EduCourse? {
    val course = project.course as? EduCourse ?: return null
    if (!course.isStudy || course.isPreview) return null
    if (!course.isMarketplaceRemote || course.isFromCourseStorage()) return null
    return course
  }

  private fun completedPercent(course: Course): Int {
    CourseCertificatePercentageService.getInstance(project).completedPercent?.let { return it }
    val (tasksSolved, tasksTotalNum) = ProgressUtil.countProgress(course)
    if (tasksTotalNum == 0) return 0
    return tasksSolved * 100 / tasksTotalNum
  }

  override fun cleanUpState() {
    state = CourseCertificationState.Unknown
  }

  companion object {
    private val LOG: Logger = logger<CourseCertificateManager>()

    fun getInstance(project: Project): CourseCertificateManager = project.service()
  }
}

sealed interface CourseCertificationState {

  val isFinal: Boolean
    get() = this is Unsupported || this is Issued && !isFirstClientRequest // otherwise `isFirstClientRequest` might never be true

  val hasCertification: Boolean
    get() = this is Supported || this is Issued

  data object Unknown : CourseCertificationState
  data object Unsupported : CourseCertificationState
  data class Supported(val requiredPercent: Int) : CourseCertificationState
  data class Issued(val certificateId: String, val isFirstClientRequest: Boolean) : CourseCertificationState
}
