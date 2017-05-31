package com.jetbrains.edu.kotlin.android;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.learning.newproject.ui.EduCoursesPanel;
import com.jetbrains.edu.learning.newproject.ui.EduCreateNewProjectDialog;
import com.jetbrains.edu.utils.EduIntellijUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinIcons;

import java.io.File;

public class KotlinAndroidCourseAction extends AnAction {
  public static final String DEFAULT_COURSE_NAME = "Kotlin Android Course";   // TODO: course name
  public static final String DEFAULT_COURSE_PATH = "Course.zip";        // TODO: Real course
  private String ANDROID_STUDIO = "AndroidStudio";                            // TODO: better way
  private String COURSE_LANGUAGE = "kotlin";                                  // TODO: kotlin-android fake lang
  private static final Logger LOG = Logger.getInstance(KotlinAndroidCourseAction.class);

  public KotlinAndroidCourseAction() {
    super(DEFAULT_COURSE_NAME, DEFAULT_COURSE_NAME, KotlinIcons.SMALL_LOGO);
  }

  @Override
  public void update(AnActionEvent e) {
    final String platformPrefix = PlatformUtils.getPlatformPrefix();
    final Presentation presentation = e.getPresentation();
    if (!ANDROID_STUDIO.equals(platformPrefix)) {
      presentation.setEnabledAndVisible(false);
    } else {
      presentation.setEnabledAndVisible(true);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    File courseRoot = EduIntellijUtils.getBundledCourseRoot(DEFAULT_COURSE_PATH, KotlinAndroidCourseAction.class);
    final Course course = StudyProjectGenerator.getCourse(FileUtil.join(courseRoot.getPath(), DEFAULT_COURSE_PATH));
    if (course == null) {
      LOG.info("Failed to find course " + DEFAULT_COURSE_PATH);
      return;
    }
    course.setLanguage(COURSE_LANGUAGE);

    final String location = EduCoursesPanel.nameToLocation(DEFAULT_COURSE_NAME);
    EduCreateNewProjectDialog.createProject(EduPluginConfigurator.INSTANCE.
        forLanguage(course.getLanguageById()).getEduCourseProjectGenerator(), course, location);

  }
}
