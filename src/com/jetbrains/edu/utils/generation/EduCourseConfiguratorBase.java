package com.jetbrains.edu.utils.generation;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.learning.intellij.EduCourseConfigurator;
import com.jetbrains.edu.learning.stepic.CourseInfo;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class EduCourseConfiguratorBase implements EduCourseConfigurator {
  private static final Logger LOG = Logger.getInstance(EduCourseConfiguratorBase.class);

  @Override
  public void createCourseFromCourseInfo(@NotNull ModifiableModuleModel moduleModel, @NotNull Project project, @NotNull StudyProjectGenerator generator, @NotNull CourseInfo selectedCourse, @Nullable String moduleDir) {
    try {
      EduModuleBuilderUtils.createCourseFromCourseInfo(moduleModel, project, generator, selectedCourse, moduleDir);
    } catch (JDOMException | ModuleWithNameAlreadyExists | ConfigurationException | IOException e) {
      LOG.error(e);
    }
  }
}
