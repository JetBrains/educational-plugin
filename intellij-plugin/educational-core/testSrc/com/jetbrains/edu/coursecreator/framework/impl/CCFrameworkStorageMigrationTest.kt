package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.coursecreator.framework.storage.CCFrameworkStorage
import com.jetbrains.edu.coursecreator.framework.storage.migration.CCUserChanges0
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test

class CCFrameworkStorageMigrationTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  override fun setUpProject() {
    val course = course {}
    createCourseStructure(course)
  }

  @Test
  fun `test migrate from 0 to 1`() {
    val storage = createStorage()
    val oldState = mapOf(
      "a.kt" to "fun foo() {}",
      "c.kt" to "fun bar() {}"
    )
    val oldChanges = CCUserChanges0(oldState)

    val record = storage.createRecordWithData(oldChanges)
    storage.migrate(CCFrameworkLessonManager.STORAGE_VERSION)

   val newState = storage.getState(record).state

    assertEquals(oldState.size, newState.size)
    for ((key, oldValue) in oldState) {
      val contents = newState[key]
      assertEquals(oldValue, contents?.textualRepresentation)
    }
  }

  private fun createStorage(): CCFrameworkStorage {
    val storage = CCFrameworkStorage(CCFrameworkLessonManager.constructStoragePath(project))
    Disposer.register(testRootDisposable, storage)
    return storage
  }
}
