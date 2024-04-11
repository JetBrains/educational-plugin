package com.jetbrains.edu.learning

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.TestOnly

class MockEduBrowser : EduBrowser() {
  var lastVisitedUrl: String? = null

  override fun browse(link: String) {
    logger<MockEduBrowser>().info("Opening $link in browser")
    lastVisitedUrl = link
  }

  @TestOnly
  override fun cleanUpState() {
    lastVisitedUrl = null
  }
}