package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.hash.HashMap;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

public class ToSixthVersionXmlConverter implements XmlConverter {

  private static final Logger LOG = Logger.getInstance(ToSixthVersionXmlConverter.class);

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
        VirtualFile taskDir = getTaskDir(project, lesson, task);
        if (taskDir == null) {
          throw new StudyUnrecognizedFormatException();
        }
        List<VirtualFile> taskDescriptionFiles = Arrays.stream(taskDir.getChildren())
                .filter(file -> EduUtils.isTaskDescriptionFile(file.getName()))
                .collect(Collectors.toList());
        Map<String, String> taskTextsMap = new HashMap<>();
        for (VirtualFile file : taskDescriptionFiles) {
          try {
            String text = VfsUtilCore.loadText(file);
            String key = FileUtil.getNameWithoutExtension(file.getName());
            if (key.equals(EduNames.TASK) && taskDescriptionFiles.size() > 1) {
              taskTextsMap.put(EduNames.TASK + EduNames.SUBTASK_MARKER + 0, text);
            }
            else {
              taskTextsMap.put(key, text);
            }
          }
          catch (IOException e) {
            LOG.error(e);
          }
        }
        addTextChildMap(task, TASK_TEXTS, taskTextsMap);
      }
    }
    return element;
  }
}
