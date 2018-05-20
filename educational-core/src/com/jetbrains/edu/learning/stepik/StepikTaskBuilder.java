package com.jetbrains.edu.learning.stepik;

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
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import org.jetbrains.annotations.NonNls;
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
  @NonNls private String myName;
  private final Language myLanguage;
  private StepikWrappers.Step myStep;
  private final Map<String, Computable<Task>> stepikTaskTypes = ImmutableMap.<String, Computable<Task>>builder()
    .put("code", this::codeTask)
    .put("choice", this::choiceTask)
    .put("text", this::theoryTask)
    .put("pycharm", this::pycharmTask)
    .put("video", this::videoTask)
    .put("number", this::unsupportedTask)
    .put("sorting", this::unsupportedTask)
    .put("matching", this::unsupportedTask)
    .put("string", this::unsupportedTask)
    .put("math", this::unsupportedTask)
    .put("free-answer", this::unsupportedTask)
    .put("table", this::unsupportedTask)
    .put("dataset", this::unsupportedTask)
    .put("admin", this::unsupportedTask)
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
    return stepikTaskTypes.get(type).compute();
  }

  public boolean isSupported(String type) {
    return stepikTaskTypes.containsKey(type);
  }

  @NotNull
  private CodeTask codeTask() {
    CodeTask task = new CodeTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.position);
    task.setUpdateDate(myStepSource.update_date);

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
      String editorText = templateForTask == null ? (commentPrefix + " write your answer here \n") : templateForTask;
      String taskFileName = getTaskFileName(myLanguage);
      if (taskFileName != null) {
        createMockTaskFile(task, editorText, taskFileName);
      }
    }
    return task;
  }

  @NotNull
  private ChoiceTask choiceTask() {
    ChoiceTask task = new ChoiceTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.position);
    task.setUpdateDate(myStepSource.update_date);
    task.setDescriptionText(myStep.text);

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
      String editorText = commentPrefix + " you can experiment here, it won't be checked\n";
      final EduConfigurator<?> configurator = EduConfiguratorManager.forLanguage(myLanguage);
      if (configurator != null) {
        editorText += "\n" + configurator.getMockTemplate();
      }
      createMockTaskFile(task, editorText, taskFileName);
    }

    return task;
  }

  @NotNull
  private TheoryTask theoryTask() {
    TheoryTask task = new TheoryTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.position);
    task.setUpdateDate(myStepSource.update_date);
    task.setDescriptionText(myStep.text);
    String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(myLanguage).getLineCommentPrefix();
    String taskFileName = getTaskFileName(myLanguage);

    if (taskFileName != null) {
      String editorText = commentPrefix + " you can experiment here, it won’t be checked\n";
      final EduConfigurator<?> configurator = EduConfiguratorManager.forLanguage(myLanguage);
      if (configurator != null) {
        editorText += "\n" + configurator.getMockTemplate();
      }
      createMockTaskFile(task, editorText, taskFileName);
    }
    return task;
  }

  @NotNull
  private Task unsupportedTask() {
    TheoryTask task = new TheoryTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.position);
    final String stepText = "This is " + myName.toLowerCase() + " task.";
    task.setDescriptionText(stepText);
    createMockFileWithDefaultText(task);
    return task;
  }

  private void createMockFileWithDefaultText(@NotNull Task task) {
    String commentPrefix = LanguageCommenters.INSTANCE.forLanguage(myLanguage).getLineCommentPrefix();
    String taskFileName = getTaskFileName(myLanguage);

    if (taskFileName != null) {
      String editorText = commentPrefix + " this is a " + myName.toLowerCase() + " task. You can use this editor as a playground\n";
      final EduConfigurator<?> configurator = EduConfiguratorManager.forLanguage(myLanguage);
      if (configurator != null) {
        editorText += "\n" + configurator.getMockTemplate();
      }
      createMockTaskFile(task, editorText, taskFileName);
    }
  }

  @NotNull
  private Task videoTask() {
    VideoTask task = new VideoTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.position);
    final String stepText = "This is " + myName.toLowerCase() + " task.";
    task.setDescriptionText(stepText);
    createMockFileWithDefaultText(task);
    return task;
  }

  @Nullable
  private Task pycharmTask() {
    if (!myStep.name.startsWith(PYCHARM_PREFIX)) {
      LOG.error("Got a block with non-pycharm prefix: " + myStep.name + " for step: " + myStepId);
      return null;
    }
    Task task = createPluginTask();
    task.setStepId(myStepId);
    task.setUpdateDate(myStepSource.update_date);
    task.setName(myStep.options != null ? myStep.options.title : (PYCHARM_PREFIX + EduVersions.JSON_FORMAT_VERSION));

    for (StepikWrappers.FileWrapper wrapper : myStep.options.test) {
      task.addTestsTexts(wrapper.name, wrapper.text);
    }
    if (myStep.options.additionalFiles != null) {
      for (StepikWrappers.FileWrapper wrapper : myStep.options.additionalFiles) {
        task.addAdditionalFile(wrapper.name, wrapper.text);
      }
    }
    if (myStep.options.descriptionText != null) {
      task.setDescriptionText(myStep.options.descriptionText);
    } else {
      task.setDescriptionText(myStep.text);
    }
    if (myStep.options.descriptionFormat != null) {
      task.setDescriptionFormat(myStep.options.descriptionFormat);
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
    final String fileText = file.text;
    final List<AnswerPlaceholder> placeholders = file.getAnswerPlaceholders();
    for (AnswerPlaceholder placeholder : placeholders) {
      final int offset = placeholder.getOffset();
      final int length = placeholder.getLength();
      if (fileText.length() > offset + length) {
        placeholder.setPlaceholderText(fileText.substring(offset, offset + length));
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
