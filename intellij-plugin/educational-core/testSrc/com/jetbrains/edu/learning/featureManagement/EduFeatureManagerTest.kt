package com.jetbrains.edu.learning.featureManagement

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class EduFeatureManagerTest : EduSettingsServiceTestBase() {

  @Test
  fun `test empty state serialization`() {
    val featureManager = EduFeatureManager()
    featureManager.loadStateAndCheck("""
      <state><![CDATA[{}]]></state>
    """)
    assertEquals(emptySet<EduManagedFeature>(), featureManager.disabledFeatures.value)
  }

  @Test
  fun `test state without content is loaded as empty set`() {
    val featureManager = EduFeatureManager()
    featureManager.loadStateAndCheck("""
      <state />
    """, """
      <state><![CDATA[{}]]></state>
    """)
    assertEquals(emptySet<EduManagedFeature>(), featureManager.disabledFeatures.value)
  }

  @Test
  fun `test disabled feature serialization`() {
    val featureManager = EduFeatureManager()
    featureManager.loadStateAndCheck("""
      <state><![CDATA[{
        "disabledFeatures": [
          "AI_HINTS"
        ]
      }]]></state>
    """)
    assertEquals(setOf(EduManagedFeature.AI_HINTS), featureManager.disabledFeatures.value)
    assertTrue(featureManager.checkDisabled(EduManagedFeature.AI_HINTS))
    assertFalse(featureManager.checkDisabled(EduManagedFeature.AI_COMPLETION))
  }

  @Test
  fun `test state is serialized after update`() {
    val featureManager = EduFeatureManager()
    featureManager.updateManagerState(setOf(EduManagedFeature.AI_COMPLETION))
    featureManager.checkState("""
      <state><![CDATA[{
        "disabledFeatures": [
          "AI_COMPLETION"
        ]
      }]]></state>
    """)
  }
}
