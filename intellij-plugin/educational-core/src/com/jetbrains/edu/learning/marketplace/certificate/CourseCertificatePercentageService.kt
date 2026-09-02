package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware

/**
 * Overrides the completed course percent reported to the Learning Center.
 *
 * Intended for manual testing only: it lets QA reach the percent required for a certificate
 * without solving the whole course.
 * Set using the internal [com.jetbrains.edu.learning.marketplace.actions.SetCourseProgressAction] and kept in memory only
 */
@Service(Service.Level.PROJECT)
class CourseCertificatePercentageService : EduTestAware {

  /**
   * `null` means the actual course progress is used
   */
  @Volatile
  var completedPercent: Int? = null

  override fun cleanUpState() {
    completedPercent = null
  }

  companion object {
    fun getInstance(project: Project): CourseCertificatePercentageService = project.service()
  }
}
