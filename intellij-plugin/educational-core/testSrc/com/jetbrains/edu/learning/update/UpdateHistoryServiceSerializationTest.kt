package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class UpdateHistoryServiceSerializationTest : EduSettingsServiceTestBase() {

  @Test
  fun `test empty state serialization`() {
    val service = UpdateHistoryService()
    service.loadStateAndCheck("""
      <state><![CDATA[{}]]></state>
    """)
    assertTrue(service.isEmpty())
  }

  @Test
  fun `test state is serialized after update`() {
    val service = UpdateHistoryService()
    service.updateHappened(UpdateItem("2", "3", UpdaterImplementation.COLLECT_UPDATE))
    service.checkState("""
      <state><![CDATA[{
        "updates": [
          {
            "versionBefore": "2",
            "versionAfter": "3",
            "updater": "COLLECT_UPDATE"
          }
        ]
      }]]></state>
    """)
  }

  @Test
  fun `test non-empty state serialization`() {
    val service = UpdateHistoryService()
    service.loadStateAndCheck("""
      <state><![CDATA[{
        "updates": [
          {
            "versionBefore": "1",
            "versionAfter": "2",
            "updater": "FIRST"
          },
          {
            "versionBefore": "2",
            "versionAfter": "3",
            "updater": "COLLECT_UPDATE"
          }
        ]
      }]]></state>
    """)
    assertEquals("1->2(updater FIRST),2->3", service.updatesString())
  }

  @Test
  fun `test legacy xml state is read`() {
    val service = UpdateHistoryService()
    service.loadStateAndCheck("""
      <State>
        <option name="updates">
          <list>
            <UpdateItem />
          </list>
        </option>
      </State>
    """, """
      <state><![CDATA[{}]]></state>
    """)
    assertTrue(service.isEmpty())
  }
}
