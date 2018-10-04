package com.jetbrains.edu.learning.stepik.alt;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase;
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HyperskillCourseProjectGenerator extends GradleCourseProjectGenerator {
  private static final Logger LOG = Logger.getInstance(HyperskillCourseProjectGenerator.class);

  public HyperskillCourseProjectGenerator(@NotNull GradleCourseBuilderBase builder,
                                          @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected boolean beforeProjectGenerated() {
    try {
      List<Section> sections = HyperskillConnector.INSTANCE.getSections();
      sections.forEach(section -> myCourse.addSection(section));
      return true;
    }
    catch (Exception e) {
      // Notifications aren't able to be shown during course generating process,
      // so we just log the error and return false
      LOG.warn(e);
      return false;
    }
  }
}
