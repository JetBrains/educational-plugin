package com.jetbrains.edu.learning.projectView

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.findLesson
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CourseViewVisibleItemsSerializationTest : EduSettingsServiceTestBase() {

  @Test
  fun `test empty state serialization`() = runTest {
    val visibleItems = CourseViewVisibleItems()
    visibleItems.loadStateAndCheck("""
      <state><![CDATA[{}]]></state>
    """)
    assertFalse(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test state without content is loaded as empty state`() = runTest {
    val visibleItems = CourseViewVisibleItems()
    visibleItems.loadStateAndCheck("""
      <state />
    """, """
      <state><![CDATA[{}]]></state>
    """)
    assertFalse(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test enabled subtree mode serialization`() = runTest {
    val visibleItems = CourseViewVisibleItems()
    visibleItems.loadStateAndCheck("""
      <state><![CDATA[{
        "subtreeModeEnabled": true
      }]]></state>
    """)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test visible task ids serialization`() = runTest {
    val visibleItems = CourseViewVisibleItems()
    visibleItems.loadStateAndCheck("""
      <state><![CDATA[{
        "subtreeModeEnabled": true,
        "visibleTaskIds": [
          11,
          12
        ]
      }]]></state>
    """)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEquals(setOf(11, 12), visibleItems.visibleTaskIds)
  }

  @Test
  fun `test state is serialized after update`() = runTest {
    val course = courseWithFiles {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 11) { taskFile("Task.txt") }
        eduTask("task2", stepId = 12) { taskFile("Task.txt") }
      }
    }
    val visibleItems = CourseViewVisibleItems()
    visibleItems.isSubtreeModeEnabled = true
    visibleItems.markStudyItemAsVisible(course.findLesson("lesson1"))

    visibleItems.checkState("""
      <state><![CDATA[{
        "subtreeModeEnabled": true,
        "visibleTaskIds": [
          11,
          12
        ]
      }]]></state>
    """)
  }

  @Test
  fun `test state is reset when subtree mode is disabled`() = runTest {
    val course = courseWithFiles {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 11) { taskFile("Task.txt") }
        eduTask("task2", stepId = 12) { taskFile("Task.txt") }
      }
    }
    val visibleItems = CourseViewVisibleItems()
    visibleItems.isSubtreeModeEnabled = true
    visibleItems.markStudyItemAsVisible(course.findLesson("lesson1"))

    visibleItems.isSubtreeModeEnabled = false

    visibleItems.checkState("""
      <state><![CDATA[{}]]></state>
    """)
  }

  private fun TestScope.CourseViewVisibleItems(): CourseViewVisibleItems = CourseViewVisibleItems(project, backgroundScope)
}
