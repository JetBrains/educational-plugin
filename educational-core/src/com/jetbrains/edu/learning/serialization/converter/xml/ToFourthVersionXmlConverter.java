package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.ADDITIONAL_HINTS;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.HINT;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.POSSIBLE_ANSWER;

public class ToFourthVersionXmlConverter implements XmlConverter {

  @NotNull
  @Override
  public Element convert(@NotNull Project project, @NotNull Element element) throws StudyUnrecognizedFormatException {
    Element taskManagerElement = element.getChild(MAIN_ELEMENT);
    Element courseElement = getChildWithName(taskManagerElement, COURSE).getChild(COURSE_TITLED);
    for (Element lesson : getChildList(courseElement, LESSONS)) {
      for (Element task : getChildList(lesson, TASK_LIST)) {
        Map<String, Element> taskFiles = getChildMap(task, TASK_FILES);
        for (Map.Entry<String, Element> entry : taskFiles.entrySet()) {
          Element taskFileElement = entry.getValue();
          for (Element placeholder : getChildList(taskFileElement, ANSWER_PLACEHOLDERS)) {
            Element valueElement = new Element(SUBTASK_INFO);
            addChildMap(placeholder, SUBTASK_INFOS, Collections.singletonMap(String.valueOf(0), valueElement));
            for (String childName : ContainerUtil.list(HINT, POSSIBLE_ANSWER, SELECTED, STATUS, TASK_TEXT)) {
              Element child = getChildWithName(placeholder, childName, true);
              if (child == null) {
                continue;
              }
              valueElement.addContent(child.clone());
            }
            renameElement(getChildWithName(valueElement, TASK_TEXT), PLACEHOLDER_TEXT);
            Element hint = getChildWithName(valueElement, HINT);
            Element firstHint = new Element(OPTION).setAttribute(VALUE, hint.getAttributeValue(VALUE));
            List<Element> newHints = new ArrayList<>();
            newHints.add(firstHint);
            newHints.addAll(ContainerUtil.map(getChildList(placeholder, ADDITIONAL_HINTS, true), Element::clone));
            addChildList(valueElement, "hints", newHints);
          }
        }
      }
    }

    return element;
  }
}
