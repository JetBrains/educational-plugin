package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem

interface YamlMigrationStep {
  fun migrateCourse(config: ObjectNode): ObjectNode = config
  fun migrateSection(config: ObjectNode, parentCourse: Course): ObjectNode = config
  fun migrateLesson(config: ObjectNode, parentItem: StudyItem): ObjectNode = config
  fun migrateTask(config: ObjectNode, parentLesson: Lesson): ObjectNode = config
}