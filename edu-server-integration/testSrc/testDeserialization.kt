import java.io.File
import com.fasterxml.jackson.module.kotlin.*
import com.jetbrains.edu.serverClient.*
import com.jetbrains.edu.learning.courseFormat.*


val resPath = "edu-server-integration/testResources"


val mapper = jacksonObjectMapper()
  .addMixIn(Course::class.java, CourseMixIn::class.java)
  .addMixIn(Section::class.java, SectionMixIn::class.java)
  .addMixIn(Lesson::class.java, LessonMixIn::class.java)
  .addMixIn(StudyItem::class.java, StudyItemMixIn::class.java)


fun test_course_empty() {
  val json = File("$resPath/empty_course.json").readText()
  val course = mapper.readValue<Course>(json)
  check(course.name == "test-title")
  check(course.description == "test-summary")
  check(course.humanLanguage == "English")
  check(course.items.size == 0)
}


fun test_section_empty() {
  val json = File("$resPath/empty_section.json").readText()
  val section = mapper.readValue<Section>(json)
  check(section.name == "test-section-title")
  check(section.items.size == 0)
}


fun test_lesson_empty() {
  val json = File("$resPath/empty_lesson.json").readText()
  val lesson = mapper.readValue<Lesson>(json)
  check(lesson.name == "test-lesson-title")
  check(lesson.taskList.size == 0)
}


fun test_course_1() {
  val json = File("$resPath/course_1.json").readText()
  val course = mapper.readValue<Course>(json)
  check(course.name == "test-course-title")
  check(course.description == "test-course-summary")
  check(course.humanLanguage == "English")
  check(course.items.size == 2)
  check(course.items[0] is Section)
  check(course.items[1] is Lesson)
  check(course.items[0].name == "item-1-1")
  check(course.items[1].name == "item-1-2")
  val section = course.items[0] as Section
  check(section.items[0].name == "item-1-1-1")
  check(section.items[1].name == "item-1-1-2")
}


fun main(args: Array<String>) {
  test_course_empty()
  test_section_empty()
  test_lesson_empty()
  test_course_1()
}