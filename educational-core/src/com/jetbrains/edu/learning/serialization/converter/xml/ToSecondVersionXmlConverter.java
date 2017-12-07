package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

public class ToSecondVersionXmlConverter implements XmlConverter {

  @NotNull
  @Override
  public Element convert(@NotNull Project project, @NotNull Element element) throws StudyUnrecognizedFormatException {
    final Element oldCourseElement = element.getChild(COURSE_ELEMENT);
    Element state = new Element(MAIN_ELEMENT);

    Element course = addChildWithName(state, COURSE, oldCourseElement.clone());
    course.setName(COURSE_TITLED);

    Element author = getChildWithName(course, AUTHOR);
    String authorString = author.getAttributeValue(VALUE);
    course.removeContent(author);

    String[] names = authorString.split(" ", 2);
    Element authorElement = new Element(AUTHOR_TITLED);
    addChildWithName(authorElement, FIRST_NAME, names[0]);
    addChildWithName(authorElement, SECOND_NAME, names.length == 1 ? "" : names[1]);

    addChildList(course, AUTHORS, Collections.singletonList(authorElement));

    Element courseDirectoryElement = getChildWithName(course, RESOURCE_PATH);
    renameElement(courseDirectoryElement, COURSE_DIRECTORY);

    for (Element lesson : getChildList(course, LESSONS)) {
      incrementIndex(lesson);
      for (Element task : getChildList(lesson, TASK_LIST)) {
        incrementIndex(task);
        Map<String, Element> taskFiles = getChildMap(task, TASK_FILES);
        for (Element taskFile : taskFiles.values()) {
          renameElement(getChildWithName(taskFile, TASK_WINDOWS), ANSWER_PLACEHOLDERS);
          for (Element placeholder : getChildList(taskFile, ANSWER_PLACEHOLDERS)) {
            placeholder.setName(ANSWER_PLACEHOLDER);

            Element initialState = new Element(MY_INITIAL_STATE);
            addChildWithName(placeholder, INITIAL_STATE, initialState);
            addChildWithName(initialState, MY_LINE, getChildWithName(placeholder, MY_INITIAL_LINE).getAttributeValue(VALUE));
            addChildWithName(initialState, MY_START, getChildWithName(placeholder, MY_INITIAL_START).getAttributeValue(VALUE));
            addChildWithName(initialState, MY_LENGTH, getChildWithName(placeholder, MY_INITIAL_LENGTH).getAttributeValue(VALUE));
          }
        }
      }
    }
    element.removeContent();
    element.addContent(state);
    return element;
  }
}
