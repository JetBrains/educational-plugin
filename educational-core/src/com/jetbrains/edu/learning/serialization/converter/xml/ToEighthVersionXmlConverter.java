package com.jetbrains.edu.learning.serialization.converter.xml;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.settings.CCSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.LESSONS;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*;

public class ToEighthVersionXmlConverter implements XmlConverter {

  private static final Logger LOG = Logger.getInstance(ToEighthVersionXmlConverter.class);

  @NotNull
  @Override
  public Element convert(@NotNull Project project, @NotNull Element state) throws StudyUnrecognizedFormatException {
    Element taskManagerElement = state.getChild(MAIN_ELEMENT);
    Element courseElement = getCourseElement(taskManagerElement);

    Element courseMode = getChildWithName(courseElement, "courseMode");
    if (!courseMode.getAttributeValue(VALUE).equals(CCUtils.COURSE_MODE)) {
      return state;
    }

    for (Element lesson : getChildList(courseElement, LESSONS)) {
      for (Element task : getChildList(lesson, TASK_LIST)) {
        Map<String, String> taskTexts = getChildMap(task, TASK_TEXTS);
        VirtualFile taskDir = getTaskDir(project, lesson, task);
        if (taskDir == null) {
          throw new StudyUnrecognizedFormatException();
        }

        String extension = FileUtilRt.getExtension(EduUtils.getTaskDescriptionFileName());

        for (Map.Entry<String, String> taskDescriptionData : taskTexts.entrySet()) {
          String descriptionFileName = taskDescriptionData.getKey() + "." + extension;
          VirtualFile descriptionFile = taskDir.findChild(descriptionFileName);
          if (descriptionFile == null) {
            ApplicationManager.getApplication().runWriteAction(() -> {
              try {
                VirtualFile descriptionVirtualFile = taskDir.createChildData(StudyTaskManager.class, descriptionFileName);
                VfsUtil.saveText(descriptionVirtualFile, taskDescriptionData.getValue());
              } catch (IOException e) {
                LOG.error(e);
              }
            });
          }
        }
      }
    }
    return state;
  }
}
