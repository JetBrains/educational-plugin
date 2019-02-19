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
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.stepik.api.Attempt;
import com.jetbrains.edu.learning.stepik.api.Dataset;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.util.List;
import java.util.Map;

import static com.jetbrains.edu.learning.stepik.StepikNames.PYCHARM_PREFIX;

public class StepikTaskBuilder {
  private static final String TASK_NAME = "task";
  private static final Logger LOG = Logger.getInstance(StepikTaskBuilder.class);
  private final StepSource myStepSource;
  private int myStepId;
  private int myUserId;
  @NonNls private String myName;
  private final Language myLanguage;
  private final Lesson myLesson;
  @Nullable
  private final EduConfigurator<?> myConfigurator;
  private Step myStep;
  private final Map<String, Computable<Task>> stepikTaskTypes = ImmutableMap.<String, Computable<Task>>builder()
    .put("code", this::codeTask)
    .put("choice", this::choiceTask)
    .put("text", this::theoryTask)
    .put("string", this::theoryTask)
    .put("pycharm", this::pycharmTask)
    .put("video", this::unsupportedTask)
    .put("number", this::unsupportedTask)
    .put("sorting", this::unsupportedTask)
    .put("matching", this::unsupportedTask)
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

  public StepikTaskBuilder(@NotNull Language language, @NotNull Lesson lesson, @NotNull StepSource stepSource,
                           int stepId, int userId) {
    this(language, lesson, EMPTY_NAME, stepSource, stepId, userId);
  }


  public StepikTaskBuilder(@NotNull Language language,
                           @NotNull Lesson lesson,
                           @NotNull String name,
                           @NotNull StepSource stepSource,
                           int stepId, int userId) {
    myName = name;
    myStepSource = stepSource;
    myStep = stepSource.getBlock();
    myStepId = stepId;
    myUserId = userId;
    myLanguage = language;
    myLesson = lesson;
    myConfigurator = EduConfiguratorManager.forLanguageAndCourseType(EduNames.PYCHARM, myLanguage);
    if (myConfigurator == null) {
      LOG.warn("Cannot get configurator for a language: " + myLanguage);
    }
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
    task.setIndex(myStepSource.getPosition());
    task.setUpdateDate(myStepSource.getUpdateDate());

    task.setStatus(CheckStatus.Unchecked);
    final StringBuilder taskDescription = new StringBuilder(clearCodeBlockFromTags());
    final StepOptions options = myStep.getOptions();
    if (options != null) {
      if (options.getSamples() != null) {
        taskDescription.append("<br>");
        for (List<String> sample : options.getSamples()) {
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
      if (options.getExecutionMemoryLimit() != null && options.getExecutionTimeLimit() != null) {
        taskDescription.append("<br>").append("<b>Memory limit</b>: ").append(options.getExecutionMemoryLimit()).append(" Mb")
          .append("<br>")
          .append("<b>Time limit</b>: ").append(options.getExecutionTimeLimit()).append("s").append("<br><br>");
      }

      if (myLanguage.isKindOf(EduNames.PYTHON) && options.getSamples() != null) {
        createTestFileFromSamples(task, options.getSamples());
      }
      final String templateForTask = getCodeTemplateForTask(myLanguage, options.getCodeTemplates());
      createMockTaskFile(task, "write your answer here \n", templateForTask);
    }

    task.setDescriptionText(taskDescription.toString());
    return task;
  }

  @NotNull
  private String clearCodeBlockFromTags() {
    String text = myStep.getText();
    Document parsedText = Jsoup.parse(text);
    for (Element element : parsedText.select("code")) {
      String codeBlockWithoutTags = Jsoup.clean(element.html(), new Whitelist().addTags("br"));
      codeBlockWithoutTags = codeBlockWithoutTags.replace("<br>", "\n");
      codeBlockWithoutTags = codeBlockWithoutTags.replaceAll("[\n]+", "\n");
      element.html(codeBlockWithoutTags);
    }
    return parsedText.toString();
  }

  @NotNull
  private ChoiceTask choiceTask() {
    ChoiceTask task = new ChoiceTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.getPosition());
    task.setUpdateDate(myStepSource.getUpdateDate());
    task.setDescriptionText(clearCodeBlockFromTags());

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      final Attempt attempt = StepikCheckerConnector.getAttemptForStep(myStepId, myUserId);
      if (attempt != null) {
        final Dataset dataset = attempt.getDataset();
        if (dataset != null && dataset.getOptions() != null) {
          task.setChoiceVariants(dataset.getOptions());
          task.setMultipleChoice(dataset.isMultipleChoice());
        }
        else {
          LOG.warn("Dataset for step " + myStepId + " is null");
        }
      }
    }

    createMockTaskFile(task, "you can experiment here, it won't be checked\n");
    return task;
  }

  @NotNull
  private TheoryTask theoryTask() {
    TheoryTask task = new TheoryTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.getPosition());
    task.setUpdateDate(myStepSource.getUpdateDate());
    task.setDescriptionText(clearCodeBlockFromTags());

    createMockTaskFile(task, "you can experiment here, it won’t be checked\n");
    return task;
  }

  @NotNull
  private Task unsupportedTask() {
    TheoryTask task = new TheoryTask(myName);
    task.setStepId(myStepId);
    task.setIndex(myStepSource.getPosition());
    task.setUpdateDate(myStepSource.getUpdateDate());
    final String stepText = StringUtil.capitalize(myName.toLowerCase()) + " tasks are not supported yet. <br>" +
                            "View this step on <a href=\"" + StepikUtils.getStepikLink(task, myLesson) +"\">Stepik</a>.";
    task.setDescriptionText(stepText);

    createMockTaskFile(task, "this is a " + myName.toLowerCase() + " task. You can use this editor as a playground\n");
    return task;
  }

  @Nullable
  private Task pycharmTask() {
    if (!myStep.getName().startsWith(PYCHARM_PREFIX)) {
      LOG.error("Got a block with non-pycharm prefix: " + myStep.getName() + " for step: " + myStepId);
      return null;
    }
    Task task = createPluginTask();
    task.setStepId(myStepId);
    task.setUpdateDate(myStepSource.getUpdateDate());
    StepOptions stepOptions = myStep.getOptions();
    task.setName(stepOptions != null ? stepOptions.getTitle() : (PYCHARM_PREFIX + EduVersions.JSON_FORMAT_VERSION));

    if (stepOptions != null) {
      if (stepOptions.getDescriptionText() != null) {
        task.setDescriptionText(stepOptions.getDescriptionText());
      } else {
        task.setDescriptionText(myStep.getText());
      }
      if (stepOptions.getDescriptionFormat() != null) {
        task.setDescriptionFormat(stepOptions.getDescriptionFormat());
      }

      task.setFeedbackLink(stepOptions.getMyFeedbackLink());
      if (stepOptions.getFiles() != null) {
        for (TaskFile taskFile : stepOptions.getFiles()) {
          addPlaceholdersTexts(taskFile);
          task.addTaskFile(taskFile);
        }
      }
    }
    return task;
  }

  @NotNull
  private Task createPluginTask() {
    final StepOptions options = myStep.getOptions();
    if (options == null) {
      LOG.error("No options in step source");
      return eduTask();
    }
    String type = options.getTaskType();
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
    final StepOptions options = myStep.getOptions();
    if (options == null) return;
    final List<TaskFile> taskFiles = options.getFiles();
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
    TaskFile test = new TaskFile("tests.py", testText);
    test.setVisible(false);
    task.addTaskFile(test);
  }
}
