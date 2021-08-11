package com.jetbrains.edu.learning.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.impl.NotificationSettings;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.VirtualFileExt;
import com.jetbrains.edu.learning.checker.CheckListener;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checker.details.CheckDetailsView;
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker;
import com.jetbrains.edu.learning.checker.remote.RemoteTaskCheckerManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.CheckFeedback;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.ext.TaskFileExt;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.projectView.ProgressUtil;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckPanel;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.EduUtilsKt.showPopup;

public class CheckAction extends ActionWithProgressIcon implements DumbAware {
  @NonNls
  public static final String ACTION_ID = "Educational.Check";
  private static final Logger LOG = Logger.getInstance(CheckAction.class);
  @NonNls
  private static final String PROCESS_MESSAGE = "Check in progress";

  public CheckAction() {
    this(EduCoreBundle.lazyMessage("action.check.text"), EduCoreBundle.lazyMessage("action.check.description"));
  }

  public CheckAction(Supplier<String> dynamicText) {
    this(dynamicText, dynamicText);
  }

  public CheckAction(Supplier<String> dynamicText, Supplier<String> dynamicDescription) {
    super(dynamicText, dynamicDescription);
    setUpProcessPanel(PROCESS_MESSAGE);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    if (DumbService.isDumb(project)) {
      showPopup(e.getDataContext(), ActionUtil.getUnavailableMessage(EduCoreBundle.message("check.title"), false));
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
    Task task = VirtualFileExt.getContainingTask(virtualFile, project);
    if (task == null) {
      return;
    }
    if (!CheckActionState.getInstance(project).doLock()) {
      showPopup(e.getDataContext(), EduCoreBundle.message("action.check.already.running"));
      return;
    }
    for (CheckListener listener : CheckListener.EP_NAME.getExtensionList()) {
      listener.beforeCheck(project, task);
    }

    StudyCheckTask checkTask = new StudyCheckTask(project, task);
    if (checkTask.isHeadless()) {
      // It's hack to make checker tests work properly.
      // `com.intellij.openapi.progress.ProgressManager.run(com.intellij.openapi.progress.Task)` executes task synchronously
      // if the task run in headless environment (e.g. in unit tests).
      // It blocks EDT and any next `ApplicationManager.getApplication().invokeAndWait()` call will hang because of deadlock
      Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(() -> ProgressManager.getInstance().run(checkTask));
      //noinspection TestOnlyProblems
      EduUtils.waitAndDispatchInvocationEvents(future);
    }
    else {
      ProgressManager.getInstance().run(checkTask);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
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

    TaskFile taskFile = OpenApiExtKt.getSelectedTaskFile(project);
    if (taskFile != null) {
      final Task task = taskFile.getTask();
      final Presentation checkActionPresentation = task.getCheckAction().getTemplatePresentation();
      presentation.setText(checkActionPresentation.getText());
      presentation.setDescription(checkActionPresentation.getDescription());
    }
    if (presentation.isEnabled()) {
      boolean isInProgress = CheckActionState.getInstance(project).isLocked();
      presentation.setEnabled(!isInProgress);
      return;
    }
    if (!CCUtils.isCourseCreator(project)) {
      return;
    }
    VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
    if (virtualFile == null || FileEditorManager.getInstance(project).getSelectedTextEditor() == null) {
      return;
    }
    if (VirtualFileExt.isTestsFile(virtualFile, project)) {
      presentation.setEnabledAndVisible(true);
    }
  }

  private class StudyCheckTask extends com.intellij.openapi.progress.Task.Backgroundable {
    private final Project myProject;
    private final Task myTask;
    @Nullable private final TaskChecker<?> myChecker;
    private CheckResult myResult;
    @NonNls
    private static final String TEST_RESULTS_DISPLAY_ID = "Test Results: Run";

    public StudyCheckTask(@NotNull Project project, @NotNull Task task) {
      super(project, EduCoreBundle.message("progress.title.checking.solution"), true);
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

    private void onStarted(@NotNull ProgressIndicator indicator) {
      processStarted();
      ApplicationManager.getApplication().executeOnPooledThread(() -> showFakeProgress(indicator));
      TaskDescriptionView.getInstance(myProject).checkStarted(myTask);
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
      onStarted(indicator);
      long start = System.currentTimeMillis();
      NotificationSettings notificationSettings = turnOffTestRunnerNotifications();
      CheckResult localCheckResult = localCheck(indicator);
      ApplicationManager.getApplication()
        .invokeLater(() -> NotificationsConfigurationImpl.getInstanceImpl().changeSettings(notificationSettings));
      long end = System.currentTimeMillis();
      LOG.info(String.format("Checking of %s task took %d ms", myTask.getName(), end - start));
      if (localCheckResult.getStatus() == CheckStatus.Failed) {
        myResult = localCheckResult;
        return;
      }
      RemoteTaskChecker remoteChecker = RemoteTaskCheckerManager.remoteCheckerForTask(myProject, myTask);
      myResult = remoteChecker == null ? localCheckResult : remoteChecker.check(myProject, myTask, indicator);
    }

    @NotNull
    private CheckResult localCheck(@NotNull ProgressIndicator indicator) {
      if (myChecker == null) return CheckResult.NO_LOCAL_CHECK;
      VirtualFile taskDir = myTask.getDir(OpenApiExtKt.getCourseDir(myProject));
      if (taskDir == null) return CheckResult.NO_LOCAL_CHECK;
      List<TaskFile> testFiles = getInvisibleTestFiles();
      if (myTask.getCourse().isStudy()) {
        createTests(testFiles);
      }
      try {
        return myChecker.check(indicator);
      }
      finally {
        if (TaskExt.shouldGenerateTestsOnTheFly(myTask)) {
          deleteTests(testFiles);
        }
      }
    }

    private void deleteTests(List<TaskFile> testFiles) {
      ApplicationManager.getApplication().invokeAndWait(() -> {
        for (TaskFile file : testFiles) {
          replaceFileText(file, "");
        }
      });
    }

    private void createTests(List<TaskFile> testFiles) {
      ApplicationManager.getApplication().invokeAndWait(() -> {
        for (TaskFile file : testFiles) {
          replaceFileText(file, file.getText());
        }
      });
    }

    private void replaceFileText(@NotNull final TaskFile file, @NotNull final String newText) {
      String newDocumentText = StringUtil.convertLineSeparators(newText);
      CommandProcessor.getInstance().runUndoTransparentAction(
        () -> ApplicationManager.getApplication().runWriteAction(() -> {
          Document document = TaskFileExt.getDocument(file, myProject);
          if (document == null) return;
          CommandProcessor.getInstance().executeCommand(myProject,
                                                        () -> document.setText(newDocumentText), "Change Test Text", "Edu Actions");
          PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        })
      );
    }

    @NotNull
    private List<TaskFile> getInvisibleTestFiles() {
      return myTask.getTaskFiles().values().stream()
          .filter(it -> EduUtils.isTestsFile(myTask, it.getName()) &&
                        !it.isVisible() &&
                        (myTask instanceof EduTask || myTask instanceof OutputTask))
          .collect(Collectors.toList());
    }

    private void showFakeProgress(ProgressIndicator indicator) {
      indicator.setIndeterminate(false);
      indicator.setFraction(0.01);
      try {
        while (indicator.isRunning()) {
          Thread.sleep(1000);
          double fraction = indicator.getFraction();
          indicator.setFraction(fraction + (1 - fraction) * 0.2);
        }
      }
      catch (InterruptedException ignore) {
      }
    }

    @Override
    public void onSuccess() {
      CheckStatus status = myResult.getStatus();
      if (myTask.getCourse().isStudy()) {
        myTask.setStatus(status);
        myTask.setFeedback(new CheckFeedback(new Date(), myResult));
        YamlFormatSynchronizer.saveItem(myTask);
      }
      if (myChecker != null) {
        if (status == CheckStatus.Failed) {
          myChecker.onTaskFailed();
        }
        else if (status == CheckStatus.Solved) {
          myChecker.onTaskSolved();
        }
      }
      EduCounterUsageCollector.checkTask(myTask.getStatus());
      TaskDescriptionView.getInstance(myProject).checkFinished(myTask, myResult);
      ApplicationManager.getApplication().invokeLater(() -> {
        ProgressUtil.updateCourseProgress(myProject);
        ProjectView.getInstance(myProject).refresh();

        for (CheckListener listener : CheckListener.EP_NAME.getExtensions()) {
          listener.afterCheck(myProject, myTask, myResult);
        }
      });
    }

    @Override
    public void onCancel() {
      TaskDescriptionView.getInstance(myProject).readyToCheck();
    }

    @Override
    public void onFinished() {
      if (myChecker != null) {
        myChecker.clearState();
      }
      processFinished();
      CheckActionState.getInstance(myProject).unlock();
    }

    @Override
    public void onThrowable(@NotNull Throwable error) {
      super.onThrowable(error);
      myResult = CheckResult.getFailedToCheck();
      TaskDescriptionView.getInstance(myProject).checkFinished(myTask, myResult);
    }

    private NotificationSettings turnOffTestRunnerNotifications() {
      NotificationsConfigurationImpl notificationsConfiguration = NotificationsConfigurationImpl.getInstanceImpl();
      NotificationSettings testRunnerSettings = NotificationsConfigurationImpl.getSettings(TEST_RESULTS_DISPLAY_ID);
      notificationsConfiguration.changeSettings(TEST_RESULTS_DISPLAY_ID, NotificationDisplayType.NONE, false, false);
      return testRunnerSettings;
    }
  }

  @Service
  private static final class CheckActionState {
    private final AtomicBoolean isBusy = new AtomicBoolean(false);

    boolean doLock() {
      return isBusy.compareAndSet(false, true);
    }

    boolean isLocked() {
      return isBusy.get();
    }

    void unlock() {
      isBusy.set(false);
    }

    static CheckActionState getInstance(@NotNull Project project) {
      return project.getService(CheckActionState.class);
    }
  }
}