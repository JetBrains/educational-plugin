package com.jetbrains.edu.learning.stepic;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import com.jetbrains.edu.learning.stepik.StepikAdaptiveConnector;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikWrappers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.edu.learning.stepik.StepikNames.PYCHARM_PREFIX;

public class StepikTaskBuilder {
  private static final String TASK_NAME = "task";
  private static final Logger LOG = Logger.getInstance(StepikTaskBuilder.class);
  private final StepikWrappers.StepSource myStepSource;
  private int myStepId;
  private int myUserId;
  private String myName;
  private final Language myLanguage;
  private StepikWrappers.Step myStep;
  private final Map<String, Computable<Task>> taskTypes = ImmutableMap.of(
    "code", this::codeTask,
    "choice", this::choiceTask,
    "text", this::theoryTask,
    "pycharm", this::pycharmTask
  );
  private static final Map<String, String> DEFAULT_NAMES = ImmutableMap.of(
          "code", "Programming",
          "choice", "Choice",
          "text", "Theory",
          "pycharm", "Programming"
  );
  private static final String EMPTY_NAME = "";

  public StepikTaskBuilder(@NotNull RemoteCourse course,
                           @NotNull StepikWrappers.StepSource stepSource,
                           int stepId, int userId) {
    this(course, EMPTY_NAME, stepSource, stepId, userId);
  }


  public StepikTaskBuilder(@NotNull RemoteCourse course,
                           @NotNull String name,
                           @NotNull StepikWrappers.StepSource stepSource,
                           int stepId, int userId) {
    myName = name;
    myStepSource = stepSource;
    myStep = stepSource.block;
    myStepId = stepId;
    myUserId = userId;
    myLanguage = course.getLanguageById();
  }

  @Nullable
  public Task createTask(String type) {
    myName = myName == EMPTY_NAME ? DEFAULT_NAMES.get(type) : myName;
    return taskTypes.get(type).compute();
  }

  public boolean isSupported(String type) {
    return taskTypes.containsKey(type);
  }

  @NotNull
  private CodeTask codeTask() {
    CodeTask task = new CodeTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.position);

    task.setStatus(CheckStatus.Unchecked);
    final StringBuilder taskDescription = new StringBuilder(myStep.text);
    if (myStep.options.samples != null) {
      taskDescription.append("<br>");
      for (List<String> sample : myStep.options.samples) {
        if (sample.size() == 2) {
          taskDescription.append("<b>Sample Input:</b><br>");
          taskDescription.append(StringUtil.replace(sample.get(0), "\n", "<br>"));
          taskDescription.append("<br>");
          taskDescription.append("<b>Sample Output:</b><br>");
          taskDescription.append(StringUtil.replace(sample.get(1), "\n", "<br>"));
          taskDescription.append("<br><br>");
        }
      }
    }

    if (myStep.options.executionMemoryLimit != null && myStep.options.executionTimeLimit != null) {
      taskDescription.append("<br>").append("<b>Memory limit</b>: ").append(myStep.options.executionMemoryLimit).append(" Mb")
        .append("<br>")
        .append("<b>Time limit</b>: ").append(myStep.options.executionTimeLimit).append("s").append("<br><br>");
    }
    task.addTaskText(EduNames.TASK, taskDescription.toString());

    if (myStep.options.test != null) {
      for (StepikWrappers.FileWrapper wrapper : myStep.options.test) {
        task.addTestsTexts(wrapper.name, wrapper.text);
      }
    }
    else {
      if (myLanguage.isKindOf(EduNames.PYTHON) && myStep.options.samples != null) {
        createTestFileFromSamples(task, myStep.options.samples);
      }
    }

    task.taskFiles = new HashMap<>();
    if (myStep.options.files != null) {
      for (TaskFile taskFile : myStep.options.files) {
        task.taskFiles.put(taskFile.name, taskFile);
      }
    }
    else {
      final String templateForTask = getCodeTemplateForTask(myLanguage, myStep.options.codeTemplates);
      String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(myLanguage).getLineCommentPrefix();
      String text = templateForTask == null ? (commentPrefix + " write your answer here \n") : templateForTask;
      String taskFileName = getTaskFileName(myLanguage);
      if (taskFileName != null) {
        createMockTaskFile(task, text, taskFileName);
      }
    }
    return task;
  }

  @NotNull
  private ChoiceTask choiceTask() {
    ChoiceTask task = new ChoiceTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.position);
    task.addTaskText(EduNames.TASK, myStep.text);

    final StepikWrappers.AdaptiveAttemptWrapper.Attempt attempt = StepikAdaptiveConnector.getAttemptForStep(myStepId, myUserId);
    if (attempt != null) {
      final StepikWrappers.AdaptiveAttemptWrapper.Dataset dataset = attempt.dataset;
      if (dataset != null) {
        task.setChoiceVariants(dataset.options);
        task.setMultipleChoice(dataset.is_multiple_choice);
      }
      else {
        LOG.warn("Dataset for step " + myStepId + " is null");
      }
    }
    String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(myLanguage).getLineCommentPrefix();
    String taskFileName = getTaskFileName(myLanguage);
    if (taskFileName != null) {
      createMockTaskFile(task, commentPrefix + " you can experiment here, it won't be checked\n", taskFileName);
    }

    return task;
  }

  @NotNull
  private TheoryTask theoryTask() {
    TheoryTask task = new TheoryTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.position);
    task.addTaskText(EduNames.TASK, myStep.text);
    String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(myLanguage).getLineCommentPrefix();
    String taskFileName = getTaskFileName(myLanguage);

    if (taskFileName != null) {
      createMockTaskFile(task, commentPrefix + " this is a theory task. You can use this editor as a playground\n", taskFileName);
    }
    return task;
  }

  @Nullable
  private Task pycharmTask() {
    if (!myStep.name.startsWith(PYCHARM_PREFIX)) {
      LOG.error("Got a block with non-pycharm prefix: " + myStep.name + " for step: " + myStepId);
      return null;
    }
    final int lastSubtaskIndex = myStep.options.lastSubtaskIndex;
    Task task = new EduTask();
    if (lastSubtaskIndex != 0) {
      task = createTaskWithSubtasks(lastSubtaskIndex);
    }
    task.setStepId(myStepId);
    task.setUpdateDate(myStepSource.update_date);
    task.setName(myStep.options != null ? myStep.options.title : (PYCHARM_PREFIX + StepikConnector.CURRENT_VERSION));

    for (StepikWrappers.FileWrapper wrapper : myStep.options.test) {
      task.addTestsTexts(wrapper.name, wrapper.text);
    }
    if (myStep.options.additionalFiles != null) {
      for (StepikWrappers.FileWrapper wrapper : myStep.options.additionalFiles) {
        task.addAdditionalFile(wrapper.name, wrapper.text);
      }
    }
    if (myStep.options.text != null) {
      for (StepikWrappers.FileWrapper wrapper : myStep.options.text) {
        task.addTaskText(wrapper.name, wrapper.text);
      }
    } else {
      task.addTaskText(EduNames.TASK, myStep.text);
    }

    task.taskFiles = new HashMap<>();      // TODO: it looks like we don't need taskFiles as map anymore
    if (myStep.options.files != null) {
      for (TaskFile taskFile : myStep.options.files) {
        addPlaceholdersTexts(taskFile);
        task.taskFiles.put(taskFile.name, taskFile);
      }
    }
    return task;
  }

  @NotNull
  private static Task createTaskWithSubtasks(int lastSubtaskIndex) {
    TaskWithSubtasks task = new TaskWithSubtasks();
    task.setLastSubtaskIndex(lastSubtaskIndex);
    return task;
  }

  private static void addPlaceholdersTexts(TaskFile file) {
    final String fileText = file.text;
    final List<AnswerPlaceholder> placeholders = file.getAnswerPlaceholders();
    for (AnswerPlaceholder placeholder : placeholders) {
      final AnswerPlaceholderSubtaskInfo info = placeholder.getActiveSubtaskInfo();
      if (info == null) {
        continue;
      }
      final int offset = placeholder.getOffset();
      final int length = placeholder.getLength();
      if (fileText.length() > offset + length) {
        info.setPlaceholderText(fileText.substring(offset, offset + length));
      }
    }
  }

  private static void createMockTaskFile(@NotNull Task task, @NotNull String editorText, @NotNull String taskFileName) {
    final TaskFile taskFile = new TaskFile();
    taskFile.text = editorText;
    taskFile.name = taskFileName;
    task.taskFiles.put(taskFile.name, taskFile);
  }

  @Nullable
  private static String getTaskFileName(@NotNull Language language) {
    // This is a hacky way to how we should name task file.
    // It's assumed that if test's name is capitalized we need to capitalize task file name too.
    EduConfigurator<?> eduConfigurator = EduConfiguratorManager.forLanguage(language);
    if (eduConfigurator == null) {
      LOG.warn("Cannot get configurator for a language: " + language);
      return null;
    }
    String testFileName = eduConfigurator.getTestFileName();
    boolean capitalize = !testFileName.isEmpty() && Character.isUpperCase(testFileName.charAt(0));

    LanguageFileType type = language.getAssociatedFileType();
    if (type == null) {
      LOG.warn("Failed to create task file name: associated file type for " + language + " is null");
      return null;
    }

    return (capitalize ? StringUtil.capitalize(TASK_NAME) : TASK_NAME) + "." + type.getDefaultExtension();
  }

  private static String getCodeTemplateForTask(@NotNull Language language,
                                               @Nullable Map codeTemplates) {
    final String languageString = StepikLanguages.langOfId(language.getID()).getLangName();
    if (languageString != null && codeTemplates != null) {
      return (String) codeTemplates.get(languageString);
    }

    return null;
  }

  private static void createTestFileFromSamples(@NotNull Task task,
                                                @NotNull List<List<String>> samples) {

    String testText = "from test_helper import check_samples\n\n" +
                      "if __name__ == '__main__':\n" +
                      "    check_samples(samples=" + new GsonBuilder().create().toJson(samples) + ")";
    task.addTestsTexts("tests.py", testText);
  }
}
