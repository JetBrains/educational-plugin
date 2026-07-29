package com.jetbrains.edu.learning.statistics.metadata

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class CourseSubmissionMetadataManagerTest : EduSettingsServiceTestBase() {

  @Test
  fun `test empty metadata serialization`() {
    val manager = CourseSubmissionMetadataManager()
    manager.loadStateAndCheck("""
      <state><![CDATA[{}]]></state>
    """)
    assertEquals(emptyMap<String, String>(), manager.metadata)
  }

  @Test
  fun `test state without content is loaded as empty metadata`() {
    val manager = CourseSubmissionMetadataManager()
    manager.loadStateAndCheck("""
      <state />
    """, """
      <state><![CDATA[{}]]></state>
    """)
    assertEquals(emptyMap<String, String>(), manager.metadata)
  }

  @Test
  fun `test metadata serialization`() {
    val manager = CourseSubmissionMetadataManager()
    manager.loadStateAndCheck("""
      <state><![CDATA[{
        "metadata": {
          "entry_point": "foo",
          "experiment_id": "123"
        }
      }]]></state>
    """)
    assertEquals(mapOf("entry_point" to "foo", "experiment_id" to "123"), manager.metadata)
  }
}
