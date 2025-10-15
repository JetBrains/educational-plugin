package com.jetbrains.edu.learning.marketplace.courseStorage.changeHost

import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.initializeCourse
import com.jetbrains.edu.rules.WithExperimentalFeature
import org.junit.Test

class CourseStorageHostUploadTest : EduTestCase() {
  @Test
  @WithExperimentalFeature(EduExperimentalFeatures.CC_COURSE_STORAGE, true)
  fun `test CC course storage host is different from learner production host`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}
    initializeCourse(project, course)

    val host = CourseStorageServiceHost.getSelectedHost(project)
    assertEquals(COURSE_STORAGE_PRODUCTION_CC_URL, host.url)
  }

  @Test
  @WithExperimentalFeature(EduExperimentalFeatures.CC_COURSE_STORAGE, false)
  fun `test CC course storage host is the same as learners without feature flag`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}
    initializeCourse(project, course)

    val host = CourseStorageServiceHost.getSelectedHost(project)
    assertEquals(CourseStorageServiceHost.PRODUCTION.url, host.url)
  }
}