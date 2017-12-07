package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

public class ToFifthVersionXmlConverter implements XmlConverter {

  @NotNull
  @Override
  public Element convert(@NotNull Project project, @NotNull Element state) throws StudyUnrecognizedFormatException {
    Element taskManagerElement = state.getChild(MAIN_ELEMENT);
    Element courseElement = getChildWithName(taskManagerElement, COURSE).getChild(COURSE_TITLED);
    final int courseId = getAsInt(courseElement, ID);
    if (courseElement != null && courseId > 0) {
      courseElement.setName(REMOTE_COURSE);
    }
    final Element adaptive = getChildWithName(courseElement, ADAPTIVE);
    for (Element lesson : getChildList(courseElement, LESSONS)) {
      for (Element task : getChildList(lesson, TASK_LIST)) {
        final Element lastSubtaskIndex = getChildWithName(task, LAST_SUBTASK_INDEX, true); //could be broken by 3->4 migration
        final Element adaptiveParams = getChildWithName(task, ADAPTIVE_TASK_PARAMETERS, true);
        Element theoryTask = getChildWithName(task, THEORY_TAG, true);
        if (theoryTask == null && adaptiveParams != null) {
          theoryTask = getChildWithName(adaptiveParams, THEORY_TAG, true);
        }
        final boolean hasAdaptiveParams = adaptiveParams != null && !adaptiveParams.getChildren().isEmpty();
        if (lastSubtaskIndex != null && Integer.valueOf(lastSubtaskIndex.getAttributeValue(VALUE)) != 0) {
          task.setName(TASK_WITH_SUBTASKS);
        }
        else if (theoryTask != null && Boolean.valueOf(theoryTask.getAttributeValue(VALUE))) {
          task.setName(THEORY_TASK);
        }
        else if (hasAdaptiveParams) {
          task.setName(CHOICE_TASK);
          final Element adaptiveParameters = adaptiveParams.getChildren().get(0);
          for (Element element : adaptiveParameters.getChildren()) {
            final Attribute name = element.getAttribute(NAME);
            if (name != null && !THEORY_TAG.equals(name.getValue())) {
              final Content elementCopy = element.clone();
              task.addContent(elementCopy);
            }
          }
        }
        else if (Boolean.valueOf(adaptive.getAttributeValue(VALUE))) {
          task.setName(CODE_TASK);
        }
        else {
          task.setName(PYCHARM_TASK);
        }
        task.removeContent(adaptiveParams);
        task.removeContent(theoryTask);
      }
    }
    return state;
  }
}
