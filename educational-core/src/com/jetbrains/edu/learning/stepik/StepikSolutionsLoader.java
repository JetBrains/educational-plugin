package com.jetbrains.edu.learning.stepik;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.progress.Task.WithResult;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.SubtaskUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionTaskAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.stepik.StepikAdaptiveConnector.EDU_TOOLS_COMMENT;
import static com.jetbrains.edu.learning.stepik.StepikConnector.*;

public class StepikSolutionsLoader implements Disposable {
  public static final String PROGRESS_ID_PREFIX = "77-";

  private static final Logger LOG = Logger.getInstance(StepikSolutionsLoader.class);
  private final HashMap<Integer, Future> myFutures = new HashMap<>();
  private final Project myProject;
  private MessageBusConnection myBusConnection;
  private Task mySelectedTask;

  protected StepikSolutionsLoader(@NotNull final Project project) {
    this.myProject = project;
  }

  public static StepikSolutionsLoader getInstance(@NotNull Project project) {
    StepikSolutionsLoader service = ServiceManager.getService(project, StepikSolutionsLoader.class);
    if (service != null) {
      service.init();
    }
    return service;
  }

  private void init() {
    EduEditor selectedEduEditor = EduUtils.getSelectedStudyEditor(myProject);
    if (selectedEduEditor != null && selectedEduEditor.getTaskFile() != null) {
      mySelectedTask = selectedEduEditor.getTaskFile().getTask();
    }
    addFileOpenListener();
  }

  public void loadSolutionsInBackground() {
    ProgressManager.getInstance().run(new Backgroundable(myProject, "Getting Tasks to Update") {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course != null) {
          loadSolutions(progressIndicator, course);
        }
      }
    });
  }

  @NotNull
  public List<Task> tasksToUpdateUnderProgress() throws Exception {
    return ProgressManager.getInstance().run(new WithResult<List<Task>, Exception>(myProject, "Updating Task Statuses", true) {
      @Override
      protected List<Task> compute(@NotNull ProgressIndicator progressIndicator) {
        progressIndicator.setIndeterminate(true);
        Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course != null) {
          return EduUtils.execCancelable(() -> tasksToUpdate(course));
        }
        return Collections.emptyList();
      }
    });
  }

  public void loadSolutionsInBackground(@NotNull List<Task> tasksToUpdate) {
    ProgressManager.getInstance().run(new Backgroundable(myProject, "Updating Solutions") {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        updateTasks(tasksToUpdate, progressIndicator);
      }
    });
  }

  public void loadSolutions(@Nullable ProgressIndicator progressIndicator, @NotNull Course course) {
    List<Task> tasksToUpdate = EduUtils.execCancelable(() -> tasksToUpdate(course));
    if (tasksToUpdate != null) {
      updateTasks(tasksToUpdate, progressIndicator);
    }
    else {
      LOG.warn("Can't get a list of tasks to update");
    }
  }

  private void updateTasks(@NotNull List<Task> tasks, @Nullable ProgressIndicator progressIndicator) {
    cancelUnfinishedTasks();
    myFutures.clear();

    List<Task> tasksToUpdate = tasks.stream()
      .filter(task -> !(task instanceof TheoryTask))
      .collect(Collectors.toList());

    CountDownLatch countDownLatch = new CountDownLatch(tasksToUpdate.size());
    for (int i = 0; i < tasksToUpdate.size(); i++) {
      final Task task = tasksToUpdate.get(i);
      final int progressIndex = i + 1;
      if (progressIndicator == null || !progressIndicator.isCanceled()) {
        Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
          boolean isSolved = task.getStatus() == CheckStatus.Solved;
          if (progressIndicator != null) {
            progressIndicator.setFraction((double)progressIndex / tasksToUpdate.size());
            progressIndicator.setText(String.format("Loading solution %d from %d", progressIndex, tasksToUpdate.size()));
          }
          loadSolution(myProject, task, isSolved);
          countDownLatch.countDown();
        });
        myFutures.put(task.getStepId(), future);
      }
      else {
        countDownLatch.countDown();
      }
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      if (mySelectedTask != null && tasksToUpdate.contains(mySelectedTask)) {
        EduEditor selectedEduEditor = EduUtils.getSelectedStudyEditor(myProject);
        assert selectedEduEditor != null;
        selectedEduEditor.showLoadingPanel();
        enableEditorWhenFutureDone(myFutures.get(mySelectedTask.getStepId()));
      }
    });

    try {
      countDownLatch.await();
      ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
        EduUtils.synchronize();
        if (mySelectedTask != null) {
          SubtaskUtils.updateUI(myProject, mySelectedTask, mySelectedTask.getTaskDir(myProject), true);
        }
      }));
      myBusConnection.disconnect();
    }
    catch (InterruptedException e) {
      LOG.warn(e.getCause());
    }
  }

  private void showActiveSubtask() {
    // subtask index is increased in switch subtask step, so we need to decrement it before
    int activeSubtaskIndex = ((TaskWithSubtasks)mySelectedTask).getActiveSubtaskIndex();
    ((TaskWithSubtasks)mySelectedTask).setActiveSubtaskIndex(Math.max(0, activeSubtaskIndex - 1));

    Collection<TaskFile> taskFiles = mySelectedTask.taskFiles.values();
    for (TaskFile file : taskFiles) {
      file.setTrackChanges(false);
    }

    SubtaskUtils.switchStep(myProject, (TaskWithSubtasks)mySelectedTask, activeSubtaskIndex);

    for (TaskFile file : taskFiles) {
      file.setTrackChanges(true);
    }
  }

  private void cancelUnfinishedTasks() {
    for (Future future : myFutures.values()) {
      if (!future.isDone()) {
        future.cancel(true);
      }
    }
  }

  public List<Task> tasksToUpdate(@NotNull Course course) {
    List<Task> tasksToUpdate = new ArrayList<>();
    Task[] allTasks = course.getLessons().stream().flatMap(lesson -> lesson.getTaskList().stream()).toArray(Task[]::new);

    String[] progresses = Arrays.stream(allTasks).map(task -> PROGRESS_ID_PREFIX + String.valueOf(task.getStepId())).toArray(String[]::new);
    Boolean[] taskStatuses = taskStatuses(progresses);
    if (taskStatuses == null) return tasksToUpdate;
    for (int j = 0; j < allTasks.length; j++) {
      Boolean isSolved = taskStatuses[j];
      Task task = allTasks[j];
      boolean toUpdate = false;
      if (isSolved != null && !(task instanceof TheoryTask)) {
        toUpdate = isToUpdate(task, isSolved, task.getStatus(), task.getStepId());
      }
      if (toUpdate) {
        if (task instanceof TaskWithSubtasks && isSolved) {
          // task isSolved on Stepik if and only if all subtasks were solved
          ((TaskWithSubtasks)task).setActiveSubtaskIndex(((TaskWithSubtasks)task).getLastSubtaskIndex());
        }
        task.setStatus(checkStatus(task, isSolved));
        tasksToUpdate.add(task);
      }
    }
    return tasksToUpdate;
  }

  private static CheckStatus checkStatus(@NotNull Task task, boolean solved) {
    if (!solved && task instanceof TaskWithSubtasks) {
      if (((TaskWithSubtasks)task).getActiveSubtaskIndex() > 0) {
        return CheckStatus.Unchecked;
      }
    }

    return solved ? CheckStatus.Solved : CheckStatus.Failed;
  }

  private void addFileOpenListener() {
    myBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        EduEditor eduEditor = EduUtils.getSelectedStudyEditor(myProject);
        TaskFile taskFile = EduUtils.getTaskFile(myProject, file);
        if (eduEditor != null && taskFile != null) {
          mySelectedTask = taskFile.getTask();
          Task task = taskFile.getTask();
          if (myFutures.containsKey(task.getStepId())) {
            eduEditor.showLoadingPanel();
            Future future = myFutures.get(task.getStepId());
            if (!future.isDone() || !future.isCancelled()) {
              enableEditorWhenFutureDone(future);
            }
          }
        }
      }
    });
  }

  private void enableEditorWhenFutureDone(@NotNull Future future) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        future.get();
        ApplicationManager.getApplication().invokeLater(() -> {
          EduEditor selectedEditor = EduUtils.getSelectedStudyEditor(myProject);
          if (selectedEditor != null && mySelectedTask.getTaskFiles().containsKey(selectedEditor.getTaskFile().name)) {
            JBLoadingPanel component = selectedEditor.getComponent();
            component.stopLoading();
            ((EditorImpl)selectedEditor.getEditor()).setViewer(false);
            selectedEditor.validateTaskFile();
          }
        });
      }
      catch (InterruptedException | ExecutionException e) {
        LOG.warn(e.getCause());
      }
    });
  }

  private static boolean isToUpdate(Task task, @NotNull Boolean isSolved, @NotNull CheckStatus currentStatus, int stepId) {
    if (isSolved && currentStatus != CheckStatus.Solved) {
      return true;
    }
    else if (!isSolved) {
      try {
        if (task instanceof EduTask) {
          StepikWrappers.Reply reply = getLastSubmission(String.valueOf(stepId), isSolved);
          if (reply != null && !reply.solution.isEmpty()) {
            return true;
          }
        }
        else {
          HashMap<String, String> solution = getSolutionForStepikAssignment(task, isSolved);
          if (!solution.isEmpty()) {
            return true;
          }
        }
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }

    return false;
  }

  private void loadSolution(@NotNull Project project, @NotNull Task task, boolean isSolved) {
    try {
      Map<String, String> solutionText = loadSolution(task, isSolved);
      if (solutionText.isEmpty()) return;
      updateFiles(project, task, solutionText);
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
  }

  private static Map<String, String> loadSolution(@NotNull Task task, boolean isSolved) throws IOException {
    if (task instanceof EduTask) {
      return getEduTaskSolution(task, isSolved);
    }
    else {
      return getStepikTaskSolution(task, isSolved);
    }
  }

  private static HashMap<String, String> getStepikTaskSolution(@NotNull Task task, boolean isSolved) throws IOException {
    HashMap<String, String> solutions = getSolutionForStepikAssignment(task, isSolved);
    if (!solutions.isEmpty()) {
      for (Map.Entry<String, String> entry : solutions.entrySet()) {
        String solutionWithoutEduPrefix = removeEduPrefix(task, entry.getValue());
        solutions.put(entry.getKey(), solutionWithoutEduPrefix);
      }
      task.setStatus(isSolved ? CheckStatus.Solved : CheckStatus.Failed);
    }
    return solutions;
  }

  private static Map<String, String> getEduTaskSolution(@NotNull Task task, boolean isSolved) throws IOException {
    StepikWrappers.Reply reply = getLastSubmission(String.valueOf(task.getStepId()), isSolved);
    if (reply == null || reply.solution.isEmpty()) {
      task.setStatus(CheckStatus.Unchecked);
      return Collections.emptyMap();
    }

    String serializedTask = reply.edu_task;
    if (serializedTask == null) {
      task.setStatus(checkStatus(task, isSolved));
      return loadSolutionTheOldWay(task, reply);
    }

    StepikWrappers.TaskWrapper updatedTask = new GsonBuilder()
      .registerTypeAdapter(Task.class, new StepikSubmissionTaskAdapter())
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create()
      .fromJson(serializedTask, StepikWrappers.TaskWrapper.class);

    if (updatedTask == null || updatedTask.task == null) {
      return Collections.emptyMap();
    }

    if (task instanceof TaskWithSubtasks && updatedTask.task instanceof TaskWithSubtasks) {
      ((TaskWithSubtasks)task).setActiveSubtaskIndex(((TaskWithSubtasks)updatedTask.task).getActiveSubtaskIndex());
    }
    task.setStatus(checkStatus(updatedTask.task, isSolved));

    Map<String, String> taskFileToText = new HashMap<>();
    for (StepikWrappers.SolutionFile file : reply.solution) {
      TaskFile taskFile = task.getTaskFile(file.name);
      TaskFile updatedTaskFile = updatedTask.task.getTaskFile(file.name);
      if (taskFile != null && updatedTaskFile != null) {
        setPlaceholders(taskFile, updatedTaskFile);
        taskFileToText.put(file.name, removeAllTags(file.text));
      }
    }
    return taskFileToText;
  }

  private static void setPlaceholders(@NotNull TaskFile taskFile, @NotNull TaskFile updatedTaskFile) {
    List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    List<AnswerPlaceholder> updatedPlaceholders = updatedTaskFile.getAnswerPlaceholders();
    for (int i = 0; i < answerPlaceholders.size(); i++) {
      AnswerPlaceholder answerPlaceholder = answerPlaceholders.get(i);
      AnswerPlaceholder updatedPlaceholder = updatedPlaceholders.get(i);
      answerPlaceholder.setSubtaskInfos(updatedPlaceholder.getSubtaskInfos());
      answerPlaceholder.setOffset(updatedPlaceholder.getOffset());
      answerPlaceholder.setLength(updatedPlaceholder.getLength());
      answerPlaceholder.setSelected(updatedPlaceholder.getSelected());
    }
  }

  /**
   * Before we decided to store the information about placeholders as a separate field of Stepik reply{@link StepikWrappers.Reply#edu_task},
   * we used to pass full text of task file marking placeholders with <placeholder> </placeholder> tags
   */
  private static Map<String, String> loadSolutionTheOldWay(@NotNull Task task, @NotNull StepikWrappers.Reply reply) {
    HashMap<String, String> taskFileToText = new HashMap<>();
    List<StepikWrappers.SolutionFile> solutionFiles = reply.solution;
    if (solutionFiles.isEmpty()) {
      task.setStatus(CheckStatus.Unchecked);
      return taskFileToText;
    }

    for (StepikWrappers.SolutionFile file : solutionFiles) {
      TaskFile taskFile = task.getTaskFile(file.name);
      if (taskFile != null) {
        if (setPlaceholdersFromTags(taskFile, file)) {
          taskFileToText.put(file.name, removeAllTags(file.text));
        }
        else {
          taskFileToText.put(file.name, file.text);
        }
      }
    }

    return taskFileToText;
  }

  private static String removeEduPrefix(@NotNull Task task, String solution) {
    Language language = task.getLesson().getCourse().getLanguageById();
    String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(language).getLineCommentPrefix();
    if (solution.contains(commentPrefix + EDU_TOOLS_COMMENT)) {
      return solution.replace(commentPrefix + EDU_TOOLS_COMMENT, "");
    }
    return solution;
  }

  private void updateFiles(@NotNull Project project, @NotNull Task task, Map<String, String> solutionText) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    ApplicationManager.getApplication().invokeAndWait(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      // we can switch subtask before we load document change, because otherwise placeholder text is inserted twice.
      // see com.jetbrains.edu.learning.SubtaskUtils.updatePlaceholderTexts()
      if (task instanceof TaskWithSubtasks && task.equals(mySelectedTask)) {
        showActiveSubtask();
      }
      for (TaskFile taskFile : task.getTaskFiles().values()) {
        VirtualFile vFile = taskDir.findFileByRelativePath(taskFile.name);
        if (vFile != null) {
          try {
            taskFile.setTrackChanges(false);
            VfsUtil.saveText(vFile, solutionText.get(taskFile.name));
            SaveAndSyncHandler.getInstance().refreshOpenFiles();
            taskFile.setTrackChanges(true);
          }
          catch (IOException e) {
            LOG.warn(e.getMessage());
          }
        }
      }
    }));
  }

  @Override
  public void dispose() {
    myBusConnection.disconnect();
    cancelUnfinishedTasks();
  }

  @TestOnly
  public void doLoadSolution(Task task, boolean isSolved) {
    loadSolution(myProject, task, isSolved);
  }
}
