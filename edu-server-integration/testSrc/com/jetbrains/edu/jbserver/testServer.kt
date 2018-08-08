package com.jetbrains.edu.jbserver

val service = EduServerApi.create()

fun getCoursesList() {
  val courseList = service.getCourses().execute().body() ?: CourseList()
  courseList.courses.forEach {
    it.print()
    println()
  }
}

fun getCourseMaterials(pk: Int) {
  val course = service.getCourseMaterials(pk).execute().body()
  course!!.print()
}


fun getSections(pks: List<Int>) {
  val sections = service.getSections(pks.pks()).execute().body()
  sections!!.sections.forEach {
    it.print()
  }
}

fun getLessons(pks: List<Int>) {
  val lessons = service.getLessons(pks.pks()).execute().body()
  lessons!!.lessons.forEach {
    it.print()
  }
}

fun getTasks(pks: List<Int>) {
  val task = service.getTasks(pks.pks()).execute().body()
  task!!.tasks.forEach {
    it.print()
  }
}


fun main(args: Array<String>) {

  // getCoursesList()
  // getCourseMaterials(37)
  // getSections(listOf(23,30))
  // getLessons(listOf(27,31))
  // getTasks(listOf(29,33))

}