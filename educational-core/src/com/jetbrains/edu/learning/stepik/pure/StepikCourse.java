package com.jetbrains.edu.learning.stepik.pure;

import com.intellij.util.xmlb.XmlSerializer;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import org.jdom.Element;

public class StepikCourse extends EduCourse {
  public static StepikCourse fromRemote(EduCourse remoteCourse) {
    Element element = XmlSerializer.serialize(remoteCourse);
    StepikCourse stepikCourse = XmlSerializer.deserialize(element, StepikCourse.class);
    stepikCourse.init(null, null, true);
    return stepikCourse;
  }
}
