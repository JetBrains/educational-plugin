/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.stepik.builtInServer;

import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.util.xmlb.XmlSerializationException;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import static com.jetbrains.edu.learning.EduNames.STUDY_PROJECT_XML_PATH;
import static com.jetbrains.edu.learning.EduUtils.execCancelable;
import static com.jetbrains.edu.learning.EduUtils.navigateToStep;

public class EduBuiltInServerUtils {

  public static boolean focusOpenEduProject(int courseId, int stepId) {
    final Pair<Course, Project> courseProject = focusOpenProject(
      course -> course instanceof EduCourse && ((EduCourse)course).isRemote() && course.getId() == courseId);
    if (courseProject != null) {
      ApplicationManager.getApplication().invokeLater(() -> navigateToStep(courseProject.second, courseProject.first, stepId));
      return true;
    }
    return false;
  }

  public static Pair<Course, Project> focusOpenProject(Predicate<Course> coursePredicate) {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : openProjects) {
      if (!project.isDefault()) {
        StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
        if (studyTaskManager != null) {
          Course course = studyTaskManager.getCourse();
          if (course == null) {
            return null;
          }
          Course selectedCourse = coursePredicate.test(course) ? course : null;
          if (selectedCourse != null) {
            ApplicationManager.getApplication().invokeLater(() -> requestFocus(project));
            return Pair.create(course, project);
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private static Project openProject(@NotNull String projectPath) {
    final Project[] project = {null};
    ApplicationManager.getApplication().invokeAndWait(() -> {
      TransactionGuard.getInstance().submitTransactionAndWait(() ->
        project[0] = ProjectUtil.openProject(projectPath, null, true));
      requestFocus(project[0]);
    });
    return project[0];
  }

  public static void requestFocus(@NotNull Project project) {
    ProjectUtil.focusProjectWindow(project, false);
  }

  public static Project openRecentProject(Predicate<Course> coursePredicate) {
    RecentProjectsManagerBase recentProjectsManager = RecentProjectsManagerBase.getInstanceEx();
    if (recentProjectsManager == null) {
      return null;
    }

    RecentProjectsManagerBase.State state = recentProjectsManager.getState();
    if (state == null) {
      return null;
    }

    List<String> recentPaths = state.recentPaths;
    SAXBuilder parser = new SAXBuilder();

    for (String projectPath : recentPaths) {
      Element component = readComponent(parser, projectPath);
      if (component == null) {
        continue;
      }
      final Course course = getCourse(component);
      if (coursePredicate.test(course)) {
        return openProject(projectPath);
      }
    }
    return null;
  }

  public static boolean openRecentEduCourse(int courseId, int stepId) {
    final Project project =
      openRecentProject(course -> course instanceof EduCourse && ((EduCourse)course).isRemote() && course.getId() == courseId);
    if (project != null) {
      PropertiesComponent.getInstance().setValue(StepikNames.STEP_ID, stepId, 0);
      return true;
    }
    return false;
  }


  @Nullable
  private static Element readComponent(@NotNull SAXBuilder parser, @NotNull String projectPath) {
    Element component = null;
    try {
      String studyProjectXML = projectPath + STUDY_PROJECT_XML_PATH;
      Document xmlDoc = parser.build(new File(studyProjectXML));
      Element root = xmlDoc.getRootElement();
      component = root.getChild("component");
    }
    catch (JDOMException | IOException ignored) {
    }

    return component;
  }

  private static Course getCourse(@NotNull Element component) {
    try {
      final StudyTaskManager studyTaskManager = new StudyTaskManager();
      studyTaskManager.loadState(component);
      return studyTaskManager.getCourse();
    }
    catch (IllegalStateException | XmlSerializationException ignored) {
    }
    return null;
  }

  public static boolean createEduCourse(int courseId, int stepId) {
    ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
      execCancelable(() -> {
        EduCourse course = StepikConnector.getCourseInfo(courseId, true);
        showDialog(course, stepId);
        return null;
      });
    }, "Getting Course", true, null));

    return true;
  }

  private static void showDialog(@Nullable Course course, int stepId) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (course != null) {
        PropertiesComponent.getInstance().setValue(StepikNames.STEP_ID, stepId, 0);
        new JoinCourseDialog(course).show();
      } else {
        Messages.showErrorDialog("Can not get course info from Stepik", "Failed to Create Course");
      }
    });
  }
}
