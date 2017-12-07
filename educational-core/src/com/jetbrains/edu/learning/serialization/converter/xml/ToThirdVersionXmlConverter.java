package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.hash.HashMap;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

public class ToThirdVersionXmlConverter implements XmlConverter {

  @NotNull
  @Override
  public Element convert(@NotNull Project project, @NotNull Element element) throws StudyUnrecognizedFormatException {
    Element taskManagerElement = element.getChild(MAIN_ELEMENT);
    XMLOutputter outputter = new XMLOutputter();

    Map<String, String> placeholderTextToStatus = fillStatusMap(taskManagerElement, STUDY_STATUS_MAP, outputter);
    Map<String, String> taskFileToStatusMap = fillStatusMap(taskManagerElement, TASK_STATUS_MAP, outputter);

    Element courseElement = getChildWithName(taskManagerElement, COURSE).getChild(COURSE_TITLED);
    for (Element lesson : getChildList(courseElement, LESSONS)) {
      int lessonIndex = getAsInt(lesson, INDEX);
      for (Element task : getChildList(lesson, TASK_LIST)) {
        String taskStatus = null;
        int taskIndex = getAsInt(task, INDEX);
        Map<String, Element> taskFiles = getChildMap(task, TASK_FILES);
        for (Map.Entry<String, Element> entry : taskFiles.entrySet()) {
          Element taskFileElement = entry.getValue();
          String taskFileText = outputter.outputString(taskFileElement);
          String taskFileStatus = taskFileToStatusMap.get(taskFileText);
          if (taskFileStatus != null && (taskStatus == null || taskFileStatus.equals(CheckStatus.Failed.toString()))) {
            taskStatus = taskFileStatus;
          }
          Document document = EduUtils.getDocument(project.getBasePath(), lessonIndex, taskIndex, entry.getKey());
          if (document == null) {
            continue;
          }
          for (Element placeholder : getChildList(taskFileElement, ANSWER_PLACEHOLDERS)) {
            taskStatus = addStatus(outputter, placeholderTextToStatus, taskStatus, placeholder);
            addOffset(document, placeholder);
            addInitialState(document, placeholder);
          }
        }
        if (taskStatus != null) {
          addChildWithName(task, STATUS, taskStatus);
        }
      }
    }
    return element;
  }

  private static Map<String, String> fillStatusMap(@NotNull Element taskManagerElement,
                                                   @NotNull String mapName,
                                                   @NotNull XMLOutputter outputter) throws StudyUnrecognizedFormatException {
    Map<Element, String> sourceMap = getChildMap(taskManagerElement, mapName);
    Map<String, String> destMap = new HashMap<>();
    for (Map.Entry<Element, String> entry : sourceMap.entrySet()) {
      String status = entry.getValue();
      if (status.equals(CheckStatus.Unchecked.toString())) {
        continue;
      }
      destMap.put(outputter.outputString(entry.getKey()), status);
    }
    return destMap;
  }
}
