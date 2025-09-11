package com.jetbrains.edu.learning.framework.impl.migration

import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.framework.impl.Change
import com.jetbrains.edu.learning.framework.impl.FrameworkLessonManagerImpl
import com.jetbrains.edu.learning.framework.impl.FrameworkStorage
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test
import kotlin.test.assertIs

class FrameworkStorageMigrationTest : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  override fun setUpProject() {
    val course = course {}
    createCourseStructure(course)
  }

  @Test
  fun `test migrate from 0 to current`() {
    val storage = createStorage()
    val oldChanges = UserChanges0(listOf(Change0.addFile("foo/bar.txt", "FooBar"), Change0.changeFile("foo/baz.txt", "FooBaZ")))

    val record = storage.createRecordWithData(oldChanges)
    storage.migrate(FrameworkLessonManagerImpl.VERSION)

    val userChanges = storage.getUserChanges(record)

    assertEquals(oldChanges.changes.size, userChanges.changes.size)
    for ((oldChange, newChange) in oldChanges.changes.zip(userChanges.changes)) {
      assertEquals(oldChange.path, newChange.path)
      assertEquals(oldChange.text, newChange.contents.textualRepresentation)
    }
    assertIs<Change.AddFile>(userChanges.changes[0])
    assertIs<Change.ChangeFile>(userChanges.changes[1])

    assertEquals(-1, userChanges.timestamp)
  }

  @Test
  fun `test migrate from 0 to 1`() {
    val storage = createStorage()
    val oldChanges = UserChanges0(listOf(Change0.addFile("foo/bar.txt", "FooBar"), Change0.changeFile("foo/baz.txt", "FooBaZ")))

    val record = storage.createRecordWithData(oldChanges)
    storage.migrate(1)

    val newChanges = storage.readStream(record).use(UserChanges1::read)

    assertEquals(oldChanges.changes, newChanges.changes)
    assertEquals(-1, newChanges.timestamp)
  }

  @Test
  fun `test migrate from 1 to 2`() {
    val storage = createStorage().apply { version = 1 }
    val oldChanges = UserChanges1(listOf(Change1.addFile("foo/bar.txt", "FooBar"), Change1.changeFile("foo/baz.txt", "FooBaZ")), 12)

    val record = storage.createRecordWithData(oldChanges)
    storage.migrate(2)

    val newChanges = storage.readStream(record).use(UserChanges2::read)

    assertEquals(oldChanges.changes.size, newChanges.changes.size)
    for ((oldChange, newChange) in oldChanges.changes.zip(newChanges.changes)) {
      assertEquals(oldChange.path, newChange.path)
      assertEquals(oldChange.text, newChange.contents.textualRepresentation)
    }
    assertEquals(0, newChanges.changes[0].ordinal)
    assertEquals(2, newChanges.changes[1].ordinal)
    assertEquals(12, newChanges.timestamp)
  }

  private fun createStorage(): FrameworkStorage {
    val storage = FrameworkStorage(FrameworkLessonManagerImpl.constructStoragePath(project))
    Disposer.register(testRootDisposable, storage)
    return storage
  }
}
