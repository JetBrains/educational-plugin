package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.marketplace.MARKETPLACE_LISTED_COURSES_LINK
import org.junit.Test

class ListedCoursesLoaderTest : EduTestCase()  {

  @Test
  fun `test marketplace listed courses link`() {
    assertEquals("https://raw.githubusercontent.com/JetBrains/educational-plugin/master/marketplaceListedCourses.txt", MARKETPLACE_LISTED_COURSES_LINK)
  }
}