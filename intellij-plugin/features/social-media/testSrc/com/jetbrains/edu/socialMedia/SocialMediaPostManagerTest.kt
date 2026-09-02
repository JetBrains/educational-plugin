package com.jetbrains.edu.socialMedia

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test
import kotlinx.coroutines.test.runTest

class SocialMediaPostManagerTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization`() = runTest {
    val settings = SocialMediaPostManager(backgroundScope)
    settings.loadStateAndCheck("""
      <State>
        <askedToPost>
          <option value="123" />
        </askedToPost>
      </State>
    """)
  }
}
