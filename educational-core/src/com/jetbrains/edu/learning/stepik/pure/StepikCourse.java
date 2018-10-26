package com.jetbrains.edu.learning.stepik.pure;

import com.intellij.util.xmlb.XmlSerializer;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import org.jdom.Element;

public class StepikCourse extends RemoteCourse {
  public static StepikCourse fromRemote(RemoteCourse remoteCourse) {
    Element element = XmlSerializer.serialize(remoteCourse);
    StepikCourse stepikCourse = XmlSerializer.deserialize(element, StepikCourse.class);
    stepikCourse.init(null, null, true);
    return stepikCourse;
  }
}
