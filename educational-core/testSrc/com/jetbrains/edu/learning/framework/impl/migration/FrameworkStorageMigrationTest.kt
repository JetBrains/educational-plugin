package com.jetbrains.edu.learning.framework.impl.migration

import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.framework.impl.Change
import com.jetbrains.edu.learning.framework.impl.FrameworkLessonManagerImpl
import com.jetbrains.edu.learning.framework.impl.FrameworkStorage
import com.jetbrains.edu.learning.framework.impl.FrameworkStorageData

class FrameworkStorageMigrationTest : CourseGenerationTestBase<Unit>() {

  override val defaultSettings: Unit get() = Unit

  override fun setUpProject() {
    val course = course {}
    createCourseStructure(course)
  }

  fun `test migrate from 0 to current`() {
    val storage = FrameworkStorage(FrameworkLessonManagerImpl.constructStoragePath(project))
    val oldChanges = UserChanges0(listOf(Change.AddFile("foo/bar.txt", "FooBar")))

    val record = storage.createRecordWithData(oldChanges)
    storage.migrate(FrameworkLessonManagerImpl.VERSION)

    val userChanges = storage.getUserChanges(record)

    assertEquals(oldChanges.changes, userChanges.changes)
    assertEquals(-1, userChanges.timestamp)
  }

  fun `test migrate from 0 to 1`() {
    val storage = FrameworkStorage(FrameworkLessonManagerImpl.constructStoragePath(project))
    val oldChanges = UserChanges0(listOf(Change.AddFile("foo/bar.txt", "FooBar")))

    val record = storage.createRecordWithData(oldChanges)
    storage.migrate(1)

    val newChanges = storage.readStream(record).use(UserChanges1::read)

    assertEquals(oldChanges.changes, newChanges.changes)
    assertEquals(-1, newChanges.timestamp)
  }

  private fun FrameworkStorage.createRecordWithData(data: FrameworkStorageData): Int {
    val record = createNewRecord()
    writeStream(record).use(data::write)
    return record
  }
}
