package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

public class ToSeventhVersionXmlConverter implements XmlConverter {
  @NotNull
  @Override
  public Element convert(@NotNull Project project, @NotNull Element element) throws StudyUnrecognizedFormatException {
    Element taskManagerElement = element.getChild(MAIN_ELEMENT);
    Element courseHolder = getChildWithName(taskManagerElement, COURSE);
    Element courseElement = courseHolder.getChild(COURSE_TITLED);
    if (courseElement == null) {
      courseElement = courseHolder.getChild(REMOTE_COURSE);
      if (courseElement == null) {
        throw new StudyUnrecognizedFormatException();
      }
    }
    for (Element lesson : getChildList(courseElement, LESSONS)) {
      for (Element task : getChildList(lesson, TASK_LIST)) {
        if (task.getName().equals(PYCHARM_TASK)) {
          task.setName(EDU_TASK);
        }
      }
    }
    return element;
  }
}
