package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.LESSONS;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

public class ToSeventhVersionXmlConverter implements XmlConverter {
  @NotNull
  @Override
  public Element convert(@NotNull Project project, @NotNull Element element) throws StudyUnrecognizedFormatException {
    final Element clone = element.clone();
    Element taskManagerElement = clone.getChild(MAIN_ELEMENT);
    Element courseElement = getCourseElement(taskManagerElement);
    for (Element lesson : getChildList(courseElement, LESSONS)) {
      for (Element task : getChildList(lesson, TASK_LIST)) {
        if (task.getName().equals(PYCHARM_TASK)) {
          task.setName(EDU_TASK);
        }
      }
    }
    return clone;
  }
}
