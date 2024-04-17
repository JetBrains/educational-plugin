package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.courseFormat.messageMethod
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UIMessageTest {

  @Test
  fun `test method method`() {
    val method = messageMethod
    assertNotNull(method)
    // Call `method.invoke` directly to ensure that it actually works
    val message = method.invoke("check.no.tests", emptyArray<Any>())
    assertEquals("No tests have run", message)
  }
}
