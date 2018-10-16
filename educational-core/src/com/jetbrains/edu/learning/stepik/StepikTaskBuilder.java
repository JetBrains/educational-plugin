package com.jetbrains.edu.learning.stepik;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikTaskRemoteInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.jetbrains.edu.learning.stepik.StepikNames.PYCHARM_PREFIX;

public class StepikTaskBuilder {
  private static final String TASK_NAME = "task";
  private static final Logger LOG = Logger.getInstance(StepikTaskBuilder.class);
  private final StepikWrappers.StepSource myStepSource;
  private int myUserId;
  @NonNls private String myName;
  private final Language myLanguage;
  @Nullable
  private final EduConfigurator<?> myConfigurator;
  private StepikWrappers.Step myStep;
  private final Map<String, Computable<Task>> stepikTaskTypes = ImmutableMap.<String, Computable<Task>>builder()
    .put("code", this::codeTask)
    .put("choice", this::choiceTask)
    .put("text", this::theoryTask)
    .put("string", this::theoryTask)
    .put("pycharm", this::pycharmTask)
    .build();

  private final Map<String, Computable<Task>> pluginTaskTypes = ImmutableMap.<String, Computable<Task>>builder()
    .put("edu", StepikTaskBuilder::eduTask)
    .put("output", StepikTaskBuilder::outputTask)
    .put("ide", StepikTaskBuilder::ideTask)
    .put("theory", () -> theoryTask())
    .build();

  private static final Map<String, String> DEFAULT_NAMES = ImmutableMap.<String, String>builder()
    .put("code", "Programming")
    .put("choice", "Quiz")
    .put("text", "Theory")
    .put("pycharm", "Programming")
    .put("video", "Video")
    .put("number", "Number")
    .put("sorting", "Sorting")
    .put("matching", "Matching")
    .put("string", "Text")
    .put("math", "Math")
    .put("free-answer", "Free Response")
    .put("table", "Table")
    .put("dataset", "Data")
    .put("admin", "Linux")
    .build();
  private static final String EMPTY_NAME = "";

  public StepikTaskBuilder(@NotNull Language language,
                           @NotNull StepikWrappers.StepSource stepSource,
                           int userId) {
    this(language, EMPTY_NAME, stepSource, userId);
  }


  public StepikTaskBuilder(@NotNull Language language,
                           @NotNull String name,
                           @NotNull StepikWrappers.StepSource stepSource,
                           int userId) {
    myName = name;
    myStepSource = stepSource;
    myStep = stepSource.block;
    myUserId = userId;
    myLanguage = language;
    myConfigurator = EduConfiguratorManager.forLanguage(myLanguage);
    if (myConfigurator == null) {
      LOG.warn("Cannot get configurator for a language: " + myLanguage);
    }
  }

  @NotNull
  public Task createTask(String type) {
    myName = myName == EMPTY_NAME ? DEFAULT_NAMES.get(type) : myName;
    final Computable<Task> taskComputable = stepikTaskTypes.get(type);
    return taskComputable != null ? taskComputable.compute() : unsupportedTask();
  }

  public boolean isSupported(String type) {
    return stepikTaskTypes.containsKey(type);
  }

  @NotNull
  private CodeTask codeTask() {
    CodeTask task = new CodeTask(myName);
    initializeTask(task);

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
    task.setDescriptionText(taskDescription.toString());

    if (myLanguage.isKindOf(EduNames.PYTHON) && myStep.options.samples != null) {
      createTestFileFromSamples(task, myStep.options.samples);
    }

    final String templateForTask = getCodeTemplateForTask(myLanguage, myStep.options.codeTemplates);
    createMockTaskFile(task, "write your answer here \n", templateForTask);
    return task;
  }

  @NotNull
  private ChoiceTask choiceTask() {
    ChoiceTask task = new ChoiceTask(myName);
    initializeTask(task);
    task.setDescriptionText(myStep.text);

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      final StepikWrappers.AttemptWrapper.Attempt attempt = StepikCheckerConnector.getAttemptForStep(myStepSource.id, myUserId);
      if (attempt != null) {
        final StepikWrappers.AttemptWrapper.Dataset dataset = attempt.dataset;
        if (dataset != null) {
          task.setChoiceVariants(dataset.options);
          task.setMultipleChoice(dataset.is_multiple_choice);
        }
        else {
          LOG.warn("Dataset for step " + myStepSource.id + " is null");
        }
      }
    }

    createMockTaskFile(task, "you can experiment here, it won't be checked\n");
    return task;
  }

  @NotNull
  private TheoryTask theoryTask() {
    TheoryTask task = new TheoryTask(myName);
    initializeTask(task);
    task.setDescriptionText(myStep.text);

    createMockTaskFile(task, "you can experiment here, it won’t be checked\n");
    return task;
  }

  @NotNull
  private Task unsupportedTask() {
    TheoryTask task = new TheoryTask(myName);
    initializeTask(task);
    final String stepText = "This is " + myName.toLowerCase() + " task.";
    task.setDescriptionText(stepText);

    createMockTaskFile(task, "this is a " + myName.toLowerCase() + " task. You can use this editor as a playground\n");
    return task;
  }

  private void initializeTask(Task task) {
    task.setIndex(myStepSource.position);
    final StepikTaskRemoteInfo remoteInfo = new StepikTaskRemoteInfo();
    remoteInfo.setId(myStepSource.id);
    remoteInfo.setUpdateDate(myStepSource.update_date);
    task.setRemoteInfo(remoteInfo);
  }

  @Nullable
  private Task pycharmTask() {
    if (!myStep.name.startsWith(PYCHARM_PREFIX)) {
      LOG.error("Got a block with non-pycharm prefix: " + myStep.name + " for step: " + myStepSource.id);
      return null;
    }
    Task task = createPluginTask();
    initializeTask(task);
    StepikWrappers.StepOptions stepOptions = myStep.options;
    task.setName(stepOptions != null ? stepOptions.title : (PYCHARM_PREFIX + EduVersions.JSON_FORMAT_VERSION));

    if (stepOptions != null) {
      for (StepikWrappers.FileWrapper wrapper : stepOptions.test) {
        task.addTestsTexts(wrapper.name, wrapper.text);
      }
      if (stepOptions.additionalFiles != null) {
        task.setAdditionalFiles(stepOptions.additionalFiles);
      }
      if (stepOptions.descriptionText != null) {
        task.setDescriptionText(stepOptions.descriptionText);
      } else {
        task.setDescriptionText(myStep.text);
      }
      if (stepOptions.descriptionFormat != null) {
        task.setDescriptionFormat(stepOptions.descriptionFormat);
      }

      task.setFeedbackLink(stepOptions.myFeedbackLink);
      if (stepOptions.files != null) {
        for (TaskFile taskFile : stepOptions.files) {
          addPlaceholdersTexts(taskFile);
          task.addTaskFile(taskFile);
        }
      }
    }
    return task;
  }

  @NotNull
  private Task createPluginTask() {
    String type = myStep.options.taskType;
    if (type == null || !pluginTaskTypes.containsKey(type)) {
      return eduTask();
    }
    return pluginTaskTypes.get(type).compute();
  }

  private static Task eduTask() {
    return new EduTask();
  }

  private static Task ideTask() {
    return new IdeTask();
  }

  private static Task outputTask() {
    return new OutputTask();
  }

  private static void addPlaceholdersTexts(TaskFile file) {
    final String fileText = file.getText();
    final List<AnswerPlaceholder> placeholders = file.getAnswerPlaceholders();
    for (AnswerPlaceholder placeholder : placeholders) {
      final int offset = placeholder.getOffset();
      final int length = placeholder.getLength();
      if (fileText.length() > offset + length) {
        placeholder.setPlaceholderText(fileText.substring(offset, offset + length));
      }
    }
  }

  private void createMockTaskFile(@NotNull Task task, @NotNull String comment) {
    createMockTaskFile(task, comment, null);
  }

  private void createMockTaskFile(@NotNull Task task, @NotNull String comment, @Nullable String editorText) {
    final List<TaskFile> taskFiles = myStep.options.files;
    if (taskFiles != null && !taskFiles.isEmpty()) return;

    String taskFilePath = getTaskFilePath(myLanguage);
    if (taskFilePath == null) return;

    StringBuilder editorTextBuilder = new StringBuilder();

    if (editorText == null) {
      Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(myLanguage);
      if (commenter != null) {
        String commentPrefix = commenter.getLineCommentPrefix();
        if (commentPrefix != null) {
          editorTextBuilder.append(commentPrefix)
            .append(" ")
            .append(comment);
        }
      }

      if (myConfigurator != null) {
        editorTextBuilder.append("\n").append(myConfigurator.getMockTemplate());
      }
    } else {
      editorTextBuilder.append(editorText);
    }

    final TaskFile taskFile = new TaskFile();
    taskFile.setText(editorTextBuilder.toString());
    taskFile.setName(taskFilePath);
    task.addTaskFile(taskFile);
  }

  @Nullable
  private String getTaskFilePath(@NotNull Language language) {
    // This is a hacky way to how we should name task file.
    // It's assumed that if test's name is capitalized we need to capitalize task file name too.
    if (myConfigurator == null) {
      return null;
    }
    String testFileName = myConfigurator.getTestFileName();
    boolean capitalize = !testFileName.isEmpty() && Character.isUpperCase(testFileName.charAt(0));

    LanguageFileType type = language.getAssociatedFileType();
    if (type == null) {
      LOG.warn("Failed to create task file name: associated file type for " + language + " is null");
      return null;
    }

    String name = (capitalize ? StringUtil.capitalize(TASK_NAME) : TASK_NAME) + "." + type.getDefaultExtension();
    return GeneratorUtils.joinPaths(myConfigurator.getSourceDir(), name);
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
