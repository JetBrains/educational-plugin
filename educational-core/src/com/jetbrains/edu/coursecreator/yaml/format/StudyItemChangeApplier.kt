package com.jetbrains.edu.coursecreator.yaml.format

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Specific instance of this class applies changes from deserialized item, see [com.jetbrains.edu.coursecreator.yaml.YamlDeserializer],
 * to already existing course item.
 */
abstract class StudyItemChangeApplier<T : StudyItem> {
  abstract fun applyChanges(existingItem: T, deserializedItem: T)
}

fun <T : StudyItem> getChangeApplierForItem(project: Project, item: T): StudyItemChangeApplier<T> {
  @Suppress("UNCHECKED_CAST") //
  return when (item) {
    is Course -> CourseChangeApplier<Course>()
    is Section -> SectionChangeApplier()
    is Lesson -> LessonChangeApplier<Lesson>(project)
    is Task -> TaskChangeApplier()
    else -> error("Unexpected item type: ${item.javaClass.simpleName}")
  } as StudyItemChangeApplier<T>
}