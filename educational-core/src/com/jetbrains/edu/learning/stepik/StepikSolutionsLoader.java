package com.jetbrains.edu.learning.stepik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.framework.FrameworkLessonManager;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.stepik.api.*;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView;
import com.jetbrains.edu.learning.update.UpdateNotification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jetbrains.edu.learning.stepik.StepikCheckerConnector.EDU_TOOLS_COMMENT;

public class StepikSolutionsLoader implements Disposable {
  public static final String PROGRESS_ID_PREFIX = "77-";
  public static final String OPEN_PLACEHOLDER_TAG = "<placeholder>";
  public static final String CLOSE_PLACEHOLDER_TAG = "</placeholder>";

  private static final String NOTIFICATION_TITLE = "Outdated EduTools Plugin";
  private static final String NOTIFICATION_CONTENT = "<html>Your version of EduTools plugin is outdated to apply all solutions.\n" +
                                                     "<a href=\"\">Update plugin</a> to avoid compatibility problems.\n";

  private static final Logger LOG = Logger.getInstance(StepikSolutionsLoader.class);
  private final HashMap<Integer, Future<Boolean>> myFutures = new HashMap<>();
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
    EduEditor selectedEduEditor = EduUtils.getSelectedEduEditor(myProject);
    if (selectedEduEditor != null) {
      mySelectedTask = selectedEduEditor.getTaskFile().getTask();
    }
    addFileOpenListener();
  }
  public static void postSolution(@NotNull final Task task, boolean passed, @NotNull final Project project) {
    if (task.getStepId() <= 0) {
      return;
    }

    final Attempt attempt = StepikConnector.postAttempt(task.getStepId());
    if (attempt == null) {
      LOG.warn("Failed to post an attempt " + task.getStepId());
      return;
    }
    final ArrayList<SolutionFile> files = new ArrayList<>();
    final VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      LOG.error("Failed to find task directory " + task.getName());
      return;
    }
    for (TaskFile taskFile : task.getTaskFiles().values()) {
      final String fileName = taskFile.getName();
      final VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
      if (virtualFile != null) {
        ApplicationManager.getApplication().runReadAction(() -> {
          final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
          if (document != null) {
            String text = document.getText();
            int insertedTextLength = 0;
            StringBuilder builder = new StringBuilder(text);
            for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
              builder.insert(placeholder.getOffset() + insertedTextLength, OPEN_PLACEHOLDER_TAG);
              builder.insert(placeholder.getOffset() + insertedTextLength + placeholder.getLength() + OPEN_PLACEHOLDER_TAG.length(),
                             CLOSE_PLACEHOLDER_TAG);
              insertedTextLength += OPEN_PLACEHOLDER_TAG.length() + CLOSE_PLACEHOLDER_TAG.length();
            }
            files.add(new SolutionFile(fileName, builder.toString()));
          }
        });
      }
    }

    StepikConnector.postSubmission(passed, attempt, files, task);
  }

  public void loadSolutionsInBackground() {
    ProgressManager.getInstance().run(new Backgroundable(myProject, "Getting Tasks to Update") {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course != null) {
          loadSolutions(progressIndicator, course);
          EduUtils.updateCourseProgress(myProject);
        }
      }
    });
  }

  @VisibleForTesting
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
        Future<Boolean> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            boolean isSolved = task.getStatus() == CheckStatus.Solved;
            if (progressIndicator != null) {
              progressIndicator.setFraction((double)progressIndex / tasksToUpdate.size());
              progressIndicator.setText(String.format("Loading solution %d of %d", progressIndex, tasksToUpdate.size()));
            }
            return loadSolution(myProject, task, isSolved);
          }
          finally {
            countDownLatch.countDown();
          }
        });
        myFutures.put(task.getStepId(), future);
      }
      else {
        countDownLatch.countDown();
      }
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      if (mySelectedTask != null && tasksToUpdate.contains(mySelectedTask)) {
        EduEditor selectedEduEditor = EduUtils.getSelectedEduEditor(myProject);
        if (selectedEduEditor != null) {
          selectedEduEditor.startLoading();
          enableEditorWhenFutureDone(myFutures.get(mySelectedTask.getStepId()));
        }
      }
    });

    try {
      countDownLatch.await();
      final boolean needToShowNotification = needToShowUpdateNotification();
      ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
        if (needToShowNotification) {
          new UpdateNotification(NOTIFICATION_TITLE, NOTIFICATION_CONTENT).notify(myProject);
        }
        EduUtils.synchronize();
        if (mySelectedTask != null) {
          updateUI(myProject, mySelectedTask);
        }
      }));
      myBusConnection.disconnect();
    }
    catch (InterruptedException e) {
      LOG.warn(e);
    }
  }

  private boolean needToShowUpdateNotification() {
    return myFutures.values().stream().anyMatch(future -> {
      try {
        Boolean result = future.get();
        return result == Boolean.TRUE;
      } catch (InterruptedException | ExecutionException e) {
        LOG.warn(e);
        return false;
      }
    });
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
    Stream<Lesson> lessonsFromSection = course.getSections().stream().flatMap(section -> section.getLessons().stream());
    Stream<Lesson> allLessons = Stream.concat(lessonsFromSection, course.getLessons().stream());
    Task[] allTasks = allLessons.flatMap(lesson -> lesson.getTaskList().stream()).toArray(Task[]::new);

    List<String> progresses = Arrays.stream(allTasks).map(task -> PROGRESS_ID_PREFIX + task.getStepId()).collect(Collectors.toList());
    List<Boolean> taskStatuses = StepikMultipleRequestsConnector.INSTANCE.taskStatuses(progresses);
    if (taskStatuses == null) return tasksToUpdate;
    for (int j = 0; j < allTasks.length; j++) {
      Boolean isSolved = taskStatuses.get(j);
      Task task = allTasks[j];
      boolean toUpdate = false;
      if (isSolved != null && !(task instanceof TheoryTask)) {
        toUpdate = isToUpdate(task, isSolved, task.getStatus(), task.getStepId());
      }
      if (toUpdate) {
        task.setStatus(checkStatus(isSolved));
        tasksToUpdate.add(task);
      }
    }
    return tasksToUpdate;
  }

  private static CheckStatus checkStatus(boolean solved) {
    return solved ? CheckStatus.Solved : CheckStatus.Failed;
  }

  private void addFileOpenListener() {
    myBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        EduEditor eduEditor = EduUtils.getSelectedEduEditor(myProject);
        TaskFile taskFile = EduUtils.getTaskFile(myProject, file);
        if (eduEditor != null && taskFile != null) {
          mySelectedTask = taskFile.getTask();
          Task task = taskFile.getTask();
          if (myFutures.containsKey(task.getStepId())) {
            eduEditor.startLoading();
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
          EduEditor selectedEditor = EduUtils.getSelectedEduEditor(myProject);
          if (selectedEditor != null && mySelectedTask.getTaskFiles().containsKey(selectedEditor.getTaskFile().getName())) {
            selectedEditor.stopLoading();
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
      if (task instanceof EduTask) {
        Reply reply = StepikConnector.getLastSubmission(stepId, isSolved);
        if (reply != null && reply.getSolution() != null && !reply.getSolution().isEmpty()) {
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

    return false;
  }

  /**
   * @return true if solutions for given task are incompatible with current plugin version, false otherwise
   */
  private static boolean loadSolution(@NotNull Project project, @NotNull Task task, boolean isSolved) {
    TaskSolutions taskSolutions = loadSolutionTexts(task, isSolved);
    if (!taskSolutions.hasIncompatibleSolutions && !taskSolutions.solutions.isEmpty()) {
      updateFiles(project, task, taskSolutions.solutions);
    }
    return taskSolutions.hasIncompatibleSolutions;
  }

  private static TaskSolutions loadSolutionTexts(@NotNull Task task, boolean isSolved) {
    if (task.isToSubmitToStepik()) {
      return getEduTaskSolution(task, isSolved);
    }
    else {
      return new TaskSolutions(getStepikTaskSolution(task, isSolved));
    }
  }

  private static HashMap<String, String> getStepikTaskSolution(@NotNull Task task, boolean isSolved) {
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

  private static TaskSolutions getEduTaskSolution(@NotNull Task task, boolean isSolved) {
    String language = task.getCourse().getLanguageID();
    Reply reply = StepikConnector.getLastSubmission(task.getStepId(), isSolved);
    if (reply == null || reply.getSolution() == null || reply.getSolution().isEmpty()) {
      // https://youtrack.jetbrains.com/issue/EDU-1449
      if (reply != null && reply.getSolution() == null) {
        LOG.warn(String.format("`solution` field of reply object is null for task `%s`", task.getName()));
      }
      task.setStatus(CheckStatus.Unchecked);
      return TaskSolutions.EMPTY;
    }

    if (reply.getVersion() > EduVersions.JSON_FORMAT_VERSION) {
      // TODO: show notification with suggestion to update plugin
      LOG.warn(String.format("The plugin supports versions of submission reply not greater than %d. The current version is `%d`",
                             EduVersions.JSON_FORMAT_VERSION, reply.getVersion()));
      return TaskSolutions.INCOMPATIBLE;
    }

    String serializedTask = reply.getEduTask();
    if (serializedTask == null) {
      task.setStatus(checkStatus(isSolved));
      return new TaskSolutions(loadSolutionTheOldWay(task, reply));
    }

    final SimpleModule module = new SimpleModule();
    module.addDeserializer(Task.class, new JacksonSubmissionDeserializer(reply.getVersion(), language));
    final ObjectMapper objectMapper = StepikConnector.getObjectMapper().copy();
    objectMapper.registerModule(module);
    TaskData updatedTaskData;
    try {
      updatedTaskData = objectMapper.readValue(serializedTask, TaskData.class);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
      return TaskSolutions.EMPTY;
    }

    task.setStatus(checkStatus(isSolved));

    Map<String, String> taskFileToText = new HashMap<>();
    for (SolutionFile file : reply.getSolution()) {
      TaskFile taskFile = task.getTaskFile(file.getName());
      TaskFile updatedTaskFile = updatedTaskData.getTask().getTaskFile(file.getName());
      if (taskFile != null && updatedTaskFile != null) {
        if (taskFile.getAnswerPlaceholders().size() != updatedTaskFile.getAnswerPlaceholders().size()) continue;
        setPlaceholders(taskFile, updatedTaskFile);
        taskFileToText.put(file.getName(), removeAllTags(file.getText()));
      }
    }
    return new TaskSolutions(taskFileToText);
  }

  private static void setPlaceholders(@NotNull TaskFile taskFile, @NotNull TaskFile updatedTaskFile) {
    List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    List<AnswerPlaceholder> updatedPlaceholders = updatedTaskFile.getAnswerPlaceholders();
    for (int i = 0; i < answerPlaceholders.size(); i++) {
      AnswerPlaceholder answerPlaceholder = answerPlaceholders.get(i);
      AnswerPlaceholder updatedPlaceholder = updatedPlaceholders.get(i);
      answerPlaceholder.setHints(updatedPlaceholder.getHints());
      answerPlaceholder.setPossibleAnswer(updatedPlaceholder.getPossibleAnswer());
      answerPlaceholder.setPlaceholderText(updatedPlaceholder.getPlaceholderText());
      answerPlaceholder.setStatus(updatedPlaceholder.getStatus());
      answerPlaceholder.setOffset(updatedPlaceholder.getOffset());
      answerPlaceholder.setLength(updatedPlaceholder.getLength());
      answerPlaceholder.setSelected(updatedPlaceholder.getSelected());
      answerPlaceholder.setPlaceholderDependency(answerPlaceholder.getPlaceholderDependency());
    }
  }

  /**
   * Before we decided to store the information about placeholders as a separate field of Stepik reply{@link Reply#eduTask},
   * we used to pass full text of task file marking placeholders with <placeholder> </placeholder> tags
   */
  private static Map<String, String> loadSolutionTheOldWay(@NotNull Task task, @NotNull Reply reply) {
    HashMap<String, String> taskFileToText = new HashMap<>();
    List<SolutionFile> solutionFiles = reply.getSolution();
    if (solutionFiles == null || solutionFiles.isEmpty()) {
      task.setStatus(CheckStatus.Unchecked);
      return taskFileToText;
    }

    for (SolutionFile file : solutionFiles) {
      TaskFile taskFile = task.getTaskFile(file.getName());
      if (taskFile != null) {
        if (setPlaceholdersFromTags(taskFile, file)) {
          taskFileToText.put(file.getName(), removeAllTags(file.getText()));
        }
        else {
          taskFileToText.put(file.getName(), file.getText());
        }
      }
    }

    return taskFileToText;
  }

  /**
   * Parses solution from Stepik.
   *
   * In Stepik solution text placeholder text is wrapped in <placeholder> tags. Here we're trying to find corresponding
   * placeholder for all taskFile placeholders.
   *
   * If we can't find at least one placeholder, we mark all placeholders as invalid. Invalid placeholder isn't showing
   * and task file with such placeholders couldn't be checked.
   *
   * @param taskFile for which we're updating placeholders
   * @param solutionFile from Stepik with text of last submission
   * @return false if there're invalid placeholders
   */
  static boolean setPlaceholdersFromTags(@NotNull TaskFile taskFile, @NotNull SolutionFile solutionFile) {
    int lastIndex = 0;
    StringBuilder builder = new StringBuilder(solutionFile.getText());
    List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    boolean isPlaceholdersValid = true;
    for (AnswerPlaceholder placeholder : placeholders) {
      int start = builder.indexOf(OPEN_PLACEHOLDER_TAG, lastIndex);
      int end = builder.indexOf(CLOSE_PLACEHOLDER_TAG, start);
      if (start == -1 || end == -1) {
        isPlaceholdersValid = false;
        break;
      }
      placeholder.setOffset(start);
      String placeholderText = builder.substring(start + OPEN_PLACEHOLDER_TAG.length(), end);
      placeholder.setLength(placeholderText.length());
      builder.delete(end, end + CLOSE_PLACEHOLDER_TAG.length());
      builder.delete(start, start + OPEN_PLACEHOLDER_TAG.length());
      lastIndex = start + placeholderText.length();
    }

    if (!isPlaceholdersValid) {
      for (AnswerPlaceholder placeholder : placeholders) {
        markInvalid(placeholder);
      }
    }

    return isPlaceholdersValid;
  }

  private static void markInvalid(AnswerPlaceholder placeholder) {
    placeholder.setLength(-1);
    placeholder.setOffset(-1);
  }

  static String removeAllTags(@NotNull String text) {
    String result = text.replaceAll(OPEN_PLACEHOLDER_TAG, "");
    result = result.replaceAll(CLOSE_PLACEHOLDER_TAG, "");
    return result;
  }


  @NotNull
  static HashMap<String, String> getSolutionForStepikAssignment(@NotNull Task task, boolean isSolved) {
    HashMap<String, String> taskFileToText = new HashMap<>();
    final List<Submission> submissions = StepikConnector.getSubmissions(isSolved, task.getStepId());
    if (submissions == null) {
      return taskFileToText;
    }
    Language language = task.getLesson().getCourse().getLanguageById();
    String stepikLanguage = StepikLanguages.langOfId(language.getID()).getLangName();
    for (Submission submission : submissions) {
      Reply reply = submission.getReply();
      if (reply != null && stepikLanguage != null && stepikLanguage.equals(reply.getLanguage())) {
        Collection<TaskFile> taskFiles = task.getTaskFiles().values();
        assert taskFiles.size() == 1;
        for (TaskFile taskFile : taskFiles) {
          taskFileToText.put(taskFile.getName(), reply.getCode());
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

  private static void updateFiles(@NotNull Project project, @NotNull Task task, Map<String, String> solutionsMap) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      FrameworkLessonManager frameworkLessonManager = FrameworkLessonManager.getInstance(project);
      for (TaskFile taskFile : task.getTaskFiles().values()) {
        Lesson lesson = task.getLesson();
        if (lesson instanceof FrameworkLesson && ((FrameworkLesson)lesson).currentTask() != task) {
          frameworkLessonManager.saveExternalChanges(task, solutionsMap);
        } else {
          VirtualFile vFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
          if (vFile != null) {
            try {
              final String solutionText = solutionsMap.get(taskFile.getName());
              if (solutionText != null) {
                taskFile.setTrackChanges(false);
                VfsUtil.saveText(vFile, solutionText);
                SaveAndSyncHandler.getInstance().refreshOpenFiles();
                taskFile.setTrackChanges(true);
              }
            }
            catch (IOException e) {
              LOG.warn(e.getMessage());
            }
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

  private static void updateUI(@NotNull Project project, @NotNull Task task) {
    ProjectView.getInstance(project).refresh();
    TaskDescriptionView.getInstance(project).setCurrentTask(task);
    NavigationUtils.navigateToTask(project, task);
  }

  private static class TaskSolutions {

    public static final TaskSolutions EMPTY = new TaskSolutions(Collections.emptyMap());
    public static final TaskSolutions INCOMPATIBLE = new TaskSolutions(Collections.emptyMap(), true);

    public final Map<String, String> solutions;
    public final boolean hasIncompatibleSolutions;

    public TaskSolutions(@NotNull Map<String, String> solutions, boolean hasIncompatibleSolutions) {
      this.solutions = solutions;
      this.hasIncompatibleSolutions = hasIncompatibleSolutions;
    }

    public TaskSolutions(@NotNull Map<String, String> solutions) {
      this(solutions, false);
    }
  }
}
