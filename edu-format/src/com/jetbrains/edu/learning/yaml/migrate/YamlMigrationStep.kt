package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.ObjectMapper
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
  fun migrateCourse(mapper: ObjectMapper, config: ObjectNode): ObjectNode = config
  fun migrateSection(mapper: ObjectMapper, config: ObjectNode, parentCourse: Course, sectionFolder: String): ObjectNode = config
  fun migrateLesson(mapper: ObjectMapper, config: ObjectNode, parentItem: StudyItem, lessonFolder: String): ObjectNode = config
  fun migrateTask(mapper: ObjectMapper, config: ObjectNode, parentLesson: Lesson, taskFolder: String): ObjectNode = config
}