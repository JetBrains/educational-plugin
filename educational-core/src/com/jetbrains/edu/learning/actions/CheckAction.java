package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.*;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

public class CheckAction extends DumbAwareActionWithShortcut {
  public static final String SHORTCUT = "ctrl alt pressed ENTER";
  public static final String ACTION_ID = "Educational.Check";
  private static final String CHECK_TASK = "Check Task";
  private static final String RUN_TASK = "Run Task";
  public static final String FAILED_CHECK_LAUNCH = "Failed to launch checking";
  public static final String LOGIN_NEEDED = "Please, login to Stepik to check the task";

  protected final Ref<Boolean> myCheckInProgress = new Ref<>(false);

  public CheckAction() {
    super(CHECK_TASK,"Check current task", EducationalCoreIcons.CheckTask);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    EduUsagesCollector.taskChecked();
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    if (DumbService.isDumb(project)) {
      CheckUtils
        .showTestResultPopUp("Checking is not available while indexing is in progress", MessageType.WARNING.getPopupBackground(), project);
      return;
    }
    CheckUtils.hideTestResultsToolWindow(project);
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
    final Presentation presentation = e.getPresentation();
    EduUtils.updateAction(e);

    Project project = e.getProject();
    if (project == null) {
      return;
    }

    final EduEditor studyEditor = EduUtils.getSelectedStudyEditor(project);
    if (studyEditor != null) {
      final Task task = studyEditor.getTaskFile().getTask();
      if (task instanceof TheoryTask) {
        presentation.setIcon(AllIcons.Actions.Execute);
        presentation.setText(RUN_TASK);
        presentation.setDescription("Run current task");
      }
      else {
        presentation.setIcon(EducationalCoreIcons.CheckTask);
        presentation.setText(CHECK_TASK);
        presentation.setDescription("Check current task");
      }
    }
    if (presentation.isEnabled()) {
      updateDescription(e);
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
    if (CCUtils.isTestsFile(project, virtualFile)) {
      presentation.setEnabledAndVisible(true);
    }
  }

  private static void updateDescription(AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final Project project = e.getProject();
    if (project != null) {
      final EduEditor eduEditor = EduUtils.getSelectedStudyEditor(project);
      if (eduEditor != null) {
        final Task task = eduEditor.getTaskFile().getTask();
        if (task instanceof TheoryTask) {
          presentation.setText(task.getLesson().getCourse().isAdaptive() ? "Get Next Recommendation" : "Mark as read");
        }
        else {
          presentation.setText(CHECK_TASK);
        }
      }
    }
  }

  @NotNull
  @Override
  public String getActionId() {
    return ACTION_ID;
  }

  @Override
  public String[] getShortcuts() {
    return new String[]{SHORTCUT};
  }

  private static PerformInBackgroundOption getBackgroundOption(Task task) {
    if (task instanceof TheoryTask) {
      return PerformInBackgroundOption.ALWAYS_BACKGROUND;
    }
    else {
      return PerformInBackgroundOption.DEAF;
    }
  }

  private class StudyCheckTask extends com.intellij.openapi.progress.Task.Backgroundable {
    private final Project myProject;
    private final Task myTask;
    private final TaskChecker myChecker;
    private CheckResult myResult;

    public StudyCheckTask(@NotNull Project project, @NotNull Task task) {
      super(project, "Checking Task", true, getBackgroundOption(task));
      myProject = project;
      myTask = task;
      EduConfigurator<?> configurator = EduConfiguratorManager.forLanguage(task.getLesson().getCourse().getLanguageById());
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

      boolean isRemote = myTask.getLesson().getCourse() instanceof RemoteCourse;
      if (myChecker == null) {
        myResult = new CheckResult(CheckStatus.Unchecked, "Check for " + myTask.getTaskType() + "task isn't available");
      }
      else {
        myResult = isRemote ? checkRemoteCourse() : myChecker.check();
      }
    }

    private CheckResult checkRemoteCourse() {
      if (EduUtils.isStudentProject(myProject)) {
        CheckResult remoteCheckResult = myChecker.checkOnRemote();
        if (remoteCheckResult != CheckResult.USE_LOCAL_CHECK) {
          return remoteCheckResult;
        }
      }

      return myChecker.check();
    }

    @Override
    public void onSuccess() {
      String message = myResult.getMessage();
      CheckStatus status = myResult.getStatus();
      myTask.setStatus(status);
      switch (status) {
        case Failed:
          myChecker.onTaskFailed(message);
          break;
        case Solved:
          myChecker.onTaskSolved(message);
          break;
        default:
          CheckUtils.showTestResultPopUp(message, MessageType.WARNING.getPopupBackground(), myProject);
      }
      ApplicationManager.getApplication().invokeLater(() -> {
        EduUtils.updateToolWindows(myProject);
        ProjectView.getInstance(myProject).refresh();

        for (CheckListener listener : CheckListener.EP_NAME.getExtensions()) {
          listener.afterCheck(myProject, myTask, myResult);
        }
      });
      myChecker.clearState();
      myCheckInProgress.set(false);
    }

    @Override
    public void onCancel() {
      myChecker.clearState();
      myCheckInProgress.set(false);
    }
  }
}
