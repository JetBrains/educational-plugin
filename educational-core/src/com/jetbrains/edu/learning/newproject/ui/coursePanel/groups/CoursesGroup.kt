package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode

class CoursesGroup(val name: String, val courseInfos: List<CourseInfo>, val joinCourse: (CourseInfo, CourseMode) -> Unit)