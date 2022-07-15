package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.courseFormat.bundleClass
import com.jetbrains.edu.learning.courseFormat.messageMethod
import com.jetbrains.edu.learning.messages.EduFormatBundle
import junit.framework.TestCase

class UIMessageTest : TestCase() {

  fun `test bundle class loading`() {
    assertNotNull(bundleClass)
    assertEquals(EduFormatBundle::class.java, bundleClass)
  }

  fun `test message method`() {
    assertNotNull(messageMethod)
  }
}