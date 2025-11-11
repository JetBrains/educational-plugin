package com.jetbrains.edu.socialMedia

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class SocialMediaPostManagerTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization`() {
    val settings = SocialMediaPostManager()
    settings.loadStateAndCheck("""
      <State>
        <askedToPost>
          <option value="123" />
        </askedToPost>
      </State>
    """)
  }
}
