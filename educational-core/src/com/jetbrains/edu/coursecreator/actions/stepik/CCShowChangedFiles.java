package com.jetbrains.edu.coursecreator.actions.stepik;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever;
import com.jetbrains.edu.coursecreator.stepik.StepikChangesInfo;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader;
import com.intellij.openapi.progress.Task.Modal;
import org.jetbrains.annotations.NotNull;
import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCShowChangedFiles extends DumbAwareAction {

  private static final String INFO_CHANGED = "Info Changed";
  private static final String ADDITIONAL_INFO_CHANGED = "Additional Info Changed";
  private static final String REMOVED = "Removed";
  private static final String NEW = "New";

  public CCShowChangedFiles() {
    super("Compare with Course on Stepik", "Show changed files comparing to the course on Stepik", null);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      return;
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof EduCourse)) {
      return;
    }

    ProgressManager.getInstance().run(new Modal(project, "Computing Changes", false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        EduCourse remoteCourse = StepikConnector.getInstance().getCourseInfo(course.getId());
        if (remoteCourse == null) {
          return;
        }
        StepikCourseLoader.loadCourseStructure(remoteCourse);
        remoteCourse.init(null, null, false);

        String message = buildChangeMessage((EduCourse)course, remoteCourse, project);
        ApplicationManager.getApplication().invokeLater(
          () -> Messages.showInfoMessage(message, course.getName() + " Comparing to Stepik"));
      }
    });
  }

  @VisibleForTesting
  @NotNull
  public static String buildChangeMessage(@NotNull EduCourse course, EduCourse remoteCourse, Project project) {
    StringBuilder builder = new StringBuilder();
    StepikChangeRetriever changeRetriever = new StepikChangeRetriever(project, course, remoteCourse);
    StepikChangesInfo changedItems = changeRetriever.getChangedItems();

    if (changedItems.isCourseInfoChanged()) {
      appendChangeLine(course, builder);
    }
    if (changedItems.isCourseAdditionalInfoChanged()) {
      appendChangeLine(course, builder, ADDITIONAL_INFO_CHANGED);
    }
    for (Section section : changedItems.getNewSections()) {
      appendChangeLine(section, builder, NEW);
    }
    for (Section section : changedItems.getSectionsToDelete()) {
      appendChangeLine(section, builder, REMOVED);
    }
    for (Section section : changedItems.getSectionInfosToUpdate()) {
      appendChangeLine(section, builder, INFO_CHANGED);
    }
    for (Lesson lesson : changedItems.getNewLessons()) {
      appendChangeLine(lesson, builder, NEW);
    }
    for (Lesson lesson : changedItems.getLessonsToDelete()) {
      appendChangeLine(lesson, builder, REMOVED);
    }
    for (Lesson lesson : changedItems.getLessonsInfoToUpdate()) {
      appendChangeLine(lesson, builder, INFO_CHANGED);
    }
    for (Lesson lesson: changedItems.getLessonsAdditionalInfo()) {
      appendChangeLine(lesson, builder, ADDITIONAL_INFO_CHANGED);
    }
    for (Task task : changedItems.getNewTasks()) {
      appendChangeLine(task, builder, NEW);
    }
    for (Task task : changedItems.getTasksToDelete()) {
      appendChangeLine(task, builder, REMOVED);
    }
    for (Task task : changedItems.getTasksToUpdate()) {
      appendChangeLine(task, builder, INFO_CHANGED);
    }

    String message = builder.toString();
    if (message.isEmpty()) {
      return "No changes";
    }
    return message;
  }

  private static void appendChangeLine(@NotNull StudyItem item, @NotNull StringBuilder stringBuilder) {
    appendChangeLine(item, stringBuilder, "Changed");
  }

  private static void appendChangeLine(@NotNull StudyItem item, @NotNull StringBuilder stringBuilder, @NotNull String status) {
    stringBuilder
      .append(getPath(item))
      .append(" ")
      .append(status)
      .append("\n");
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course instanceof EduCourse && ((EduCourse)course).isRemote() && !course.isStudy()) {
      presentation.setEnabledAndVisible(true);
    }
  }

  private static String getPath(@NotNull StudyItem item) {
    ArrayList<String> parents = new ArrayList<>();
    StudyItem parent = item.getParent();
    while (!(parent instanceof Course)) {
      parents.add(parent.getName());
      parent = parent.getParent();
    }
    Collections.reverse(parents);

    String parentsLine = StringUtil.join(parents, "/");
    return parentsLine + (parentsLine.isEmpty() ? "" : "/") + item.getName();
  }
}
