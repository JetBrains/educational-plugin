package com.jetbrains.edu.coursecreator.actions.stepik;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever;
import com.jetbrains.edu.coursecreator.stepik.StepikChangesInfo;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCShowChangedFiles extends DumbAwareAction {

  @Nls
  private static String getStatusInfoChanged() {
    return EduCoreBundle.message("action.show.changed.files.status.info.changed");
  }

  @Nls
  private static String getStatusAdditionalInfoChanged() {
    return EduCoreBundle.message("action.show.changed.files.status.additional.info.changed");
  }

  @Nls
  private static String getStatusRemoved() {
    return EduCoreBundle.message("action.show.changed.files.status.removed");
  }

  @Nls
  private static String getStatusNew() {
    return EduCoreBundle.message("action.show.changed.files.status.new");
  }

  public CCShowChangedFiles() {
    super(EduCoreBundle.lazyMessage("action.show.changed.files", StepikNames.STEPIK),
          EduCoreBundle.lazyMessage("action.show.changed.files.description", StepikNames.STEPIK),
          null);
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

    ProgressManager.getInstance().run(new Modal(project, EduCoreBundle.message("action.show.changed.files.computing.changes"), false) {
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
          () -> Messages.showInfoMessage(message, EduCoreBundle.message("action.show.changed.files.comparing.to",
                                                                        StepikNames.STEPIK, course.getName())));
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
      appendChangeLine(course, builder, getStatusAdditionalInfoChanged());
    }
    for (Section section : changedItems.getNewSections()) {
      appendChangeLine(section, builder, getStatusNew());
    }
    for (Section section : changedItems.getSectionsToDelete()) {
      appendChangeLine(section, builder, getStatusRemoved());
    }
    for (Section section : changedItems.getSectionInfosToUpdate()) {
      appendChangeLine(section, builder, getStatusInfoChanged());
    }
    for (Lesson lesson : changedItems.getNewLessons()) {
      appendChangeLine(lesson, builder, getStatusNew());
    }
    for (Lesson lesson : changedItems.getLessonsToDelete()) {
      appendChangeLine(lesson, builder, getStatusRemoved());
    }
    for (Lesson lesson : changedItems.getLessonsInfoToUpdate()) {
      appendChangeLine(lesson, builder, getStatusInfoChanged());
    }
    for (Lesson lesson : changedItems.getLessonAdditionalInfosToUpdate()) {
      appendChangeLine(lesson, builder, getStatusAdditionalInfoChanged());
    }
    for (Task task : changedItems.getNewTasks()) {
      appendChangeLine(task, builder, getStatusNew());
    }
    for (Task task : changedItems.getTasksToDelete()) {
      appendChangeLine(task, builder, getStatusRemoved());
    }
    for (Task task : changedItems.getTasksToUpdate()) {
      appendChangeLine(task, builder, getStatusInfoChanged());
    }

    String message = builder.toString();
    if (message.isEmpty()) {
      return EduCoreBundle.message("action.show.changed.files.no.changes");
    }
    return message;
  }

  private static void appendChangeLine(@NotNull StudyItem item, @NotNull StringBuilder stringBuilder) {
    appendChangeLine(item, stringBuilder, EduCoreBundle.message("action.show.changed.files.status.changed"));
  }

  private static void appendChangeLine(@NotNull StudyItem item, @NotNull StringBuilder stringBuilder, @NotNull @Nls String status) {
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
