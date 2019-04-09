package com.jetbrains.edu.learning.stepik;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.learning.EduNames;
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
import java.util.function.Function;

import static com.jetbrains.edu.learning.stepik.StepikNames.PYCHARM_PREFIX;

public class StepikTaskBuilder {
  private static final Logger LOG = Logger.getInstance(StepikTaskBuilder.class);
  private final StepSource myStepSource;
  private Step myStep;
  private int myStepId;
  private int myUserId;
  private final Language myLanguage;
  private final Lesson myLesson;
  @Nullable
  private final EduConfigurator<?> myConfigurator;
  private final Map<String, Function<String, Task>> stepikTaskTypes = ImmutableMap.<String, Function<String, Task>>builder()
    .put("code", this::codeTask)
    .put("choice", this::choiceTask)
    .put("text", this::theoryTask)
    .put("string", this::theoryTask)
    .put("pycharm", (name) -> pycharmTask())
    .put("video", this::unsupportedTask)
    .put("number", this::unsupportedTask)
    .put("sorting", this::unsupportedTask)
    .put("matching", this::unsupportedTask)
    .put("math", this::unsupportedTask)
    .put("free-answer", this::unsupportedTask)
    .put("table", this::unsupportedTask)
    .put("dataset", this::unsupportedTask)
    .put("admin", this::unsupportedTask)
    .put("manual-score", this::unsupportedTask)
    .build();

  private final Map<String, Function<String, Task>> pluginTaskTypes = ImmutableMap.<String, Function<String, Task>>builder()
    .put("edu", StepikTaskBuilder::eduTask)
    .put("output", StepikTaskBuilder::outputTask)
    .put("ide", StepikTaskBuilder::ideTask)
    .put("theory", (name) -> theoryTask(name))
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
    .put("manual-score", "Manual Score")
    .build();

  private static final String DEFAULT_EDU_TASK_NAME = "Edu Task";
  private static final String UNKNOWN_TASK_NAME = "Unknown Task";

  public StepikTaskBuilder(@NotNull Language language,
                           @NotNull Lesson lesson,
                           @NotNull StepSource stepSource,
                           int stepId, int userId) {
    myStepSource = stepSource;
    myStep = stepSource.getBlock();
    myStepId = stepId;
    myUserId = userId;
    myLanguage = language;
    myLesson = lesson;
    myConfigurator = EduConfiguratorManager.findConfigurator(EduNames.PYCHARM, EduNames.DEFAULT_ENVIRONMENT, myLanguage);
    if (myConfigurator == null) {
      LOG.warn("Cannot get configurator for a language: " + myLanguage);
    }
  }

  @Nullable
  public Task createTask(@NotNull String type) {
    String taskName = DEFAULT_NAMES.get(type);
    return stepikTaskTypes.get(type).apply(taskName != null ? taskName : UNKNOWN_TASK_NAME);
  }

  public boolean isSupported(@NotNull String type) {
    return stepikTaskTypes.containsKey(type);
  }

  @NotNull
  private CodeTask codeTask(@NotNull String name) {
    CodeTask task = new CodeTask(name);
    task.setId(myStepId);
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
      Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
      String codeBlockWithoutTags = Jsoup.clean(element.html(), "", new Whitelist().addTags("br"), settings);
      codeBlockWithoutTags = codeBlockWithoutTags.replace("<br>", "\n");
      codeBlockWithoutTags = codeBlockWithoutTags.replaceAll("[\n]+", "\n");
      element.html(codeBlockWithoutTags);
    }
    return parsedText.toString();
  }

  @NotNull
  private ChoiceTask choiceTask(@NotNull String name) {
    ChoiceTask task = new ChoiceTask(name);
    task.setId(myStepId);
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
  private TheoryTask theoryTask(@NotNull String name) {
    TheoryTask task = new TheoryTask(name);
    task.setId(myStepId);
    task.setIndex(myStepSource.getPosition());
    task.setUpdateDate(myStepSource.getUpdateDate());
    task.setDescriptionText(clearCodeBlockFromTags());

    createMockTaskFile(task, "you can experiment here, it won’t be checked\n");
    return task;
  }

  @NotNull
  private Task unsupportedTask(@NotNull @NonNls String name) {
    TheoryTask task = new TheoryTask(name);
    task.setId(myStepId);
    task.setIndex(myStepSource.getPosition());
    task.setUpdateDate(myStepSource.getUpdateDate());
    final String stepText = StringUtil.capitalize(name.toLowerCase()) + " tasks are not supported yet. <br>" +
                            "View this step on <a href=\"" + StepikUtils.getStepikLink(task, myLesson) + "\">Stepik</a>.";
    task.setDescriptionText(stepText);

    createMockTaskFile(task, "this is a " + name.toLowerCase() + " task. You can use this editor as a playground\n");
    return task;
  }

  @Nullable
  private Task pycharmTask() {
    if (!myStep.getName().startsWith(PYCHARM_PREFIX)) {
      LOG.error("Got a block with non-pycharm prefix: " + myStep.getName() + " for step: " + myStepId);
      return null;
    }
    StepOptions stepOptions = myStep.getOptions();
    String taskName = DEFAULT_EDU_TASK_NAME;
    if (stepOptions != null) {
      taskName = stepOptions.getTitle() != null ? stepOptions.getTitle() : DEFAULT_EDU_TASK_NAME;
    }
    Task task = createPluginTask(taskName);
    task.setId(myStepId);
    task.setUpdateDate(myStepSource.getUpdateDate());

    if (stepOptions != null) {
      if (stepOptions.getDescriptionText() != null) {
        task.setDescriptionText(stepOptions.getDescriptionText());
      }
      else {
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
  private Task createPluginTask(@NotNull String name) {
    final StepOptions options = myStep.getOptions();
    if (options == null) {
      LOG.error("No options in step source");
      return eduTask(name);
    }
    String type = options.getTaskType();
    if (type == null || !pluginTaskTypes.containsKey(type)) {
      return eduTask(name);
    }
    return pluginTaskTypes.get(type).apply(name);
  }

  private static Task eduTask(@NotNull String name) {
    return new EduTask(name);
  }

  private static Task ideTask(@NotNull String name) {
    return new IdeTask(name);
  }

  private static Task outputTask(@NotNull String name) {
    return new OutputTask(name);
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

  private void createMockTaskFile(@NotNull Task task, @NotNull String comment, @Nullable String codeTemplate) {
    final StepOptions options = myStep.getOptions();
    if (options == null) return;
    final List<TaskFile> taskFiles = options.getFiles();
    if (taskFiles != null && !taskFiles.isEmpty()) {
      for (TaskFile file : taskFiles) {
        task.addTaskFile(file);
      }
      return;
    }

    StringBuilder editorTextBuilder = new StringBuilder();

    if (codeTemplate == null) {
      Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(myLanguage);
      if (commenter != null) {
        String commentPrefix = commenter.getLineCommentPrefix();
        if (commentPrefix != null) {
          editorTextBuilder.append(commentPrefix).append(" ").append(comment);
        }
      }

      if (myConfigurator != null) {
        editorTextBuilder.append("\n").append(myConfigurator.getMockTemplate());
      }
    }
    else {
      editorTextBuilder.append(codeTemplate);
    }

    String editorText = editorTextBuilder.toString();
    String taskFilePath = getTaskFilePath(editorText);
    if (taskFilePath == null) return;

    final TaskFile taskFile = new TaskFile();
    taskFile.setText(editorText);
    taskFile.setName(taskFilePath);
    task.addTaskFile(taskFile);
  }

  @Nullable
  private String getTaskFilePath(String editorText) {
    if (myConfigurator == null) return null;

    String fileName = myConfigurator.getMockFileName(editorText);
    if (fileName == null) return null;
    return GeneratorUtils.joinPaths(myConfigurator.getSourceDir(), fileName);
  }

  private static String getCodeTemplateForTask(@NotNull Language language,
                                               @Nullable Map codeTemplates) {
    final String languageString = StepikLanguages.langOfId(language.getID()).getLangName();
    if (languageString != null && codeTemplates != null) {
      return (String)codeTemplates.get(languageString);
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
