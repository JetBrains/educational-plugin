package com.jetbrains.edu.learning.marketplace.courseStorage

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector

class MockCourseStorageConnector : CourseStorageConnector() {
  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  override val baseUrl: String = helper.baseUrl
  override fun getRepositoryUrl(project: Project?): String = helper.baseUrl

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockCourseStorageConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }
}