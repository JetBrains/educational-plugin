package com.jetbrains.edu.learning.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.*;
import com.jetbrains.edu.learning.checker.details.CheckDetailsView;
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker;
import com.jetbrains.edu.learning.checker.remote.RemoteTaskCheckerManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.coursera.CourseraNames;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView;
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckAction extends DumbAwareAction {
  public static final String ACTION_ID = "Educational.Check";
  private static final String CHECK_TASK = "Check";
  private static final String RUN_TASK = "Run";
  private static final String RUN_DESCRIPTION = "Run current task";
  private static final String CHECK_DESCRIPTION = "Check current task";

  protected final Ref<Boolean> myCheckInProgress = new Ref<>(false);

  public CheckAction() {
    super(CHECK_TASK,"Check current task", null);
  }

  private CheckAction(String text, String description) {
    super(text, description, null);
  }

  public static CheckAction createCheckAction(@Nullable Task task) {
    if (task instanceof TheoryTask) {
      return new CheckAction(RUN_TASK, RUN_DESCRIPTION);
    }
    if (task != null && CourseraNames.COURSE_TYPE.equals(task.getCourse().getCourseType())) {
      return new CheckAction(CourseraNames.SUBMIT_TO_COURSERA, CourseraNames.SUBMIT_TO_COURSERA);
    }
    return new CheckAction(CHECK_TASK, CHECK_DESCRIPTION);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    EduUsagesCollector.taskChecked();
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    if (DumbService.isDumb(project)) {
      TaskDescriptionView.getInstance(project).showBalloon("Checking is not available while indexing is in progress", MessageType.WARNING);
      return;
    }
    CheckDetailsView.getInstance(project).clear();
    FileDocumentManager.getInstance().saveAllDocuments();
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }
    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (virtualFile == null) {
      return;
    }
    Task task = EduUtils.getTaskForFile(project, virtualFile);
    if (task == null) {
      return;
    }
    for (CheckListener listener : Extensions.getExtensions(CheckListener.EP_NAME)) {
      listener.beforeCheck(project, task);
    }
    ProgressManager.getInstance().run(new StudyCheckTask(project, task));
  }

  @Override
  public void update(AnActionEvent e) {
    if (CheckPanel.ACTION_PLACE.equals(e.getPlace())) {
      //action is being added only in valid context
      //no project in event in this case, so just enable it
      return;
    }

    final Presentation presentation = e.getPresentation();
    EduUtils.updateAction(e);

    Project project = e.getProject();
    if (project == null) {
      return;
    }

    final EduEditor studyEditor = EduUtils.getSelectedEduEditor(project);
    if (studyEditor != null) {
      final Task task = studyEditor.getTaskFile().getTask();
      if (task instanceof TheoryTask) {
        presentation.setText(RUN_TASK);
        presentation.setDescription(RUN_DESCRIPTION);
      }
      else {
        presentation.setText(CHECK_TASK);
        presentation.setDescription(CHECK_DESCRIPTION);
      }
    }
    if (presentation.isEnabled()) {
      presentation.setEnabled(!myCheckInProgress.get());
      return;
    }
    if (!CCUtils.isCourseCreator(project)) {
      return;
    }
    VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
    if (virtualFile == null || FileEditorManager.getInstance(project).getSelectedTextEditor() == null) {
      return;
    }
    if (EduUtils.isTestsFile(project, virtualFile)) {
      presentation.setEnabledAndVisible(true);
    }
  }

  private class StudyCheckTask extends com.intellij.openapi.progress.Task.Backgroundable {
    private final Project myProject;
    private final Task myTask;
    @Nullable private final TaskChecker myChecker;
    private CheckResult myResult;

    public StudyCheckTask(@NotNull Project project, @NotNull Task task) {
      super(project, "Checking Task", true);
      myProject = project;
      myTask = task;
      final Course course = task.getLesson().getCourse();
      EduConfigurator<?> configurator = CourseExt.getConfigurator(course);
      if (configurator != null) {
        TaskCheckerProvider checkerProvider = configurator.getTaskCheckerProvider();
        myChecker = checkerProvider.getTaskChecker(task, project);
      }
      else {
        myChecker = null;
      }
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
      indicator.setIndeterminate(true);
      myCheckInProgress.set(true);
      TaskDescriptionView.getInstance(myProject).checkStarted();

      CheckResult localCheckResult = myChecker == null ? CheckResult.NO_LOCAL_CHECK : myChecker.check(indicator);
      if (localCheckResult.getStatus() == CheckStatus.Failed) {
        myResult = localCheckResult;
        return;
      }
      RemoteTaskChecker remoteChecker = RemoteTaskCheckerManager.remoteCheckerForTask(myProject, myTask);
      myResult = remoteChecker == null ? localCheckResult : remoteChecker.check(myProject, myTask, indicator);
    }

    @Override
    public void onSuccess() {
      String message = myResult.getMessage();
      CheckStatus status = myResult.getStatus();
      final String details = myResult.getDetails();
      myTask.setStatus(status);
      if (status == CheckStatus.Failed) {
        if (myChecker != null) {
          myChecker.onTaskFailed(message, details);
        }
      } else if (status == CheckStatus.Solved) {
        if (myChecker != null) {
          myChecker.onTaskSolved(message);
        }
      }
      TaskDescriptionView.getInstance(myProject).checkFinished(myTask, myResult);
      ApplicationManager.getApplication().invokeLater(() -> {
        EduUtils.updateCourseProgress(myProject);
        ProjectView.getInstance(myProject).refresh();

        for (CheckListener listener : CheckListener.EP_NAME.getExtensions()) {
          listener.afterCheck(myProject, myTask, myResult);
        }
      });
      if (myChecker != null) {
        myChecker.clearState();
      }
      myCheckInProgress.set(false);
    }

    @Override
    public void onCancel() {
      if (myChecker != null) {
        myChecker.clearState();
      }
      myCheckInProgress.set(false);
      TaskDescriptionView.getInstance(myProject).readyToCheck();
    }
  }
}
