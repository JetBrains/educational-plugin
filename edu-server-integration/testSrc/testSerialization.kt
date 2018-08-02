import java.io.File
import com.fasterxml.jackson.module.kotlin.*
import com.jetbrains.edu.learning.courseFormat.*


fun test_serialize_empty_course() {
  val course = mapper.readValue<Course>(
    File("$resPath/empty_course.json").readText()
  )
  val json = mapper.writeValueAsString(course)
}


fun test_serialize_empty_section() {
  val section = mapper.readValue<Section>(
    File("$resPath/empty_section.json").readText()
  )
  val json = mapper.writeValueAsString(section)
}


fun test_serialize_empty_lesson() {
  val section = mapper.readValue<Lesson>(
    File("$resPath/empty_lesson.json").readText()
  )
  val json = mapper.writeValueAsString(section)
}


fun test_serialize_structure_course() {
  val course = mapper.readValue<Course>(
    File("$resPath/course_1.json").readText()
  )
  val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(course)
  println(json)
}


fun main(args: Array<String>) {
  test_serialize_empty_course()
  test_serialize_empty_section()
  test_serialize_empty_lesson()
  test_serialize_structure_course()
}
