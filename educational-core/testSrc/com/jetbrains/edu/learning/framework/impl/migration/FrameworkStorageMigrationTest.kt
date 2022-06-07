package com.jetbrains.edu.learning.framework.impl.migration

import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.framework.impl.Change
import com.jetbrains.edu.learning.framework.impl.FrameworkLessonManagerImpl
import com.jetbrains.edu.learning.framework.impl.FrameworkStorage

class FrameworkStorageMigrationTest : CourseGenerationTestBase<Unit>() {

  override val defaultSettings: Unit get() = Unit

  override fun setUpProject() {
    val course = course {}
    createCourseStructure(course)
  }

  fun `test migrate from 0 to current`() {
    val storage = createStorage()
    val oldChanges = UserChanges0(listOf(Change.AddFile("foo/bar.txt", "FooBar")))

    val record = storage.createRecordWithData(oldChanges)
    storage.migrate(FrameworkLessonManagerImpl.VERSION)

    val userChanges = storage.getUserChanges(record)

    assertEquals(oldChanges.changes, userChanges.changes)
    assertEquals(-1, userChanges.timestamp)
  }

  fun `test migrate from 0 to 1`() {
    val storage = createStorage()
    val oldChanges = UserChanges0(listOf(Change.AddFile("foo/bar.txt", "FooBar")))

    val record = storage.createRecordWithData(oldChanges)
    storage.migrate(1)

    val newChanges = storage.readStream(record).use(UserChanges1::read)

    assertEquals(oldChanges.changes, newChanges.changes)
    assertEquals(-1, newChanges.timestamp)
  }

  private fun createStorage(): FrameworkStorage {
    val storage = FrameworkStorage(FrameworkLessonManagerImpl.constructStoragePath(project))
    Disposer.register(testRootDisposable, storage)
    return storage
  }
}
