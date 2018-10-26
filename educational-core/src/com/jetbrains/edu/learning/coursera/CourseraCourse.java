package com.jetbrains.edu.learning.coursera;

import com.intellij.util.xmlb.XmlSerializer;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import org.jdom.Element;

public class CourseraCourse extends Course {
  public static CourseraCourse fromLocal(EduCourse course) {
    Element element = XmlSerializer.serialize(course);
    CourseraCourse courseraCourse = XmlSerializer.deserialize(element, CourseraCourse.class);
    courseraCourse.init(null, null, true);
    courseraCourse.setCourseType(CourseraNames.COURSE_TYPE);
    return courseraCourse;
  }
}
