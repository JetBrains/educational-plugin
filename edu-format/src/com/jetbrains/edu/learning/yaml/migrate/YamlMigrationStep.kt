package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Migration step does a migration from one YAML version to the next one.
 * During migration, migration methods get an already migrated and deserialized parent study item.
 * The parent item also has its parents already migrated and deserialized,
 * so during deserialization there is access to all the parents, including the course.
 */
interface YamlMigrationStep {
  fun migrateCourse(config: ObjectNode): ObjectNode = config
  fun migrateSection(config: ObjectNode, parentCourse: Course): ObjectNode = config
  fun migrateLesson(config: ObjectNode, parentItem: StudyItem): ObjectNode = config
  fun migrateTask(config: ObjectNode, parentLesson: Lesson): ObjectNode = config
}