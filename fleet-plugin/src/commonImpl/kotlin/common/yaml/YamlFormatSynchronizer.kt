package fleet.edu.common.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.YamlMapper
import fleet.api.FileAddress
import fleet.api.child
import fleet.api.createFile
import fleet.common.fs.fsService
import fleet.edu.common.format.getDir

object YamlFormatSynchronizer {
  suspend fun saveAll(course: Course, courseDir: FileAddress) {
    val mapper = course.mapper
    saveItem(course, courseDir, mapper)

    for (item in course.items) {
      if (item is Lesson) {
        saveLesson(item, courseDir, mapper)
      }
      else if (item is Section) {
        saveItem(item, courseDir, mapper)

        for (lesson in item.lessons) {
          saveLesson(lesson, courseDir, mapper)
        }
      }
    }
  }

  private suspend fun saveLesson(lesson: Lesson, courseDir: FileAddress, mapper: ObjectMapper) {
    saveItem(lesson, courseDir, mapper)
    for (task in lesson.taskList) {
      saveItem(task, courseDir, mapper)
    }
  }

  private suspend fun saveItem(item: StudyItem, courseDir: FileAddress, mapper: ObjectMapper = item.course.mapper,
                               configName: String = item.configFileName) {
    item.saveConfigDocument(courseDir, configName, mapper)
  }

  private suspend fun StudyItem.saveConfigDocument(courseDir: FileAddress, configName: String, mapper: ObjectMapper) {
    val dir = getDir(courseDir) ?: error("No config dir found")
    val fsApi = requireNotNull(fsService(dir)) { "There must be fs service for file $dir" }

    val path = dir.child(configName).path
    fsApi.createFile(path, mapper.writeValueAsBytes(this))
  }

  val Course.mapper: ObjectMapper
    get() = YamlMapper.STUDENT_MAPPER_WITH_ENCRYPTION

}
