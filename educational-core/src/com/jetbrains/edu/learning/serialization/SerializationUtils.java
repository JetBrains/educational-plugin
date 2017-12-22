package com.jetbrains.edu.learning.serialization;

import com.google.gson.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.hash.HashMap;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import com.jetbrains.edu.learning.serialization.converter.xml.*;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SerializationUtils {
  private static final Logger LOG = Logger.getInstance(SerializationUtils.class);

  public static final String PLACEHOLDERS = "placeholders";
  public static final String LINE = "line";
  public static final String START = "start";
  public static final String LENGTH = "length";
  public static final String POSSIBLE_ANSWER = "possible_answer";
  public static final String HINT = "hint";
  public static final String ADDITIONAL_HINTS = "additional_hints";
  public static final String OFFSET = "offset";
  public static final String TEXT = "text";
  public static final String LESSONS = "lessons";
  public static final String COURSE = "course";
  public static final String ID = "id";
  public static final String COURSE_TITLED = "Course";
  public static final String STATUS = "status";
  public static final String AUTHOR = "author";
  public static final String AUTHORS = "authors";
  public static final String MY_INITIAL_START = "myInitialStart";

  private SerializationUtils() {
  }

  public static class Xml {
    public final static String COURSE_ELEMENT = "courseElement";
    public final static String MAIN_ELEMENT = "StudyTaskManager";
    public final static String REMOTE_COURSE = "RemoteCourse";
    public static final String MAP = "map";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String NAME = "name";
    public static final String LIST = "list";
    public static final String OPTION = "option";
    public static final String INDEX = "index";
    public static final String STUDY_STATUS_MAP = "myStudyStatusMap";
    public static final String TASK_STATUS_MAP = "myTaskStatusMap";
    public static final String LENGTH = "length";
    public static final String ANSWER_PLACEHOLDERS = "answerPlaceholders";
    public static final String TASK_LIST = "taskList";
    public static final String TASK_FILES = "taskFiles";
    public static final String INITIAL_STATE = "initialState";
    public static final String MY_INITIAL_STATE = "MyInitialState";
    public static final String MY_LINE = "myLine";
    public static final String MY_START = "myStart";
    public static final String MY_LENGTH = "myLength";
    public static final String HINT = "hint";
    public static final String AUTHOR_TITLED = "Author";
    public static final String FIRST_NAME = "first_name";
    public static final String SECOND_NAME = "second_name";
    public static final String MY_INITIAL_LINE = "myInitialLine";
    public static final String MY_INITIAL_LENGTH = "myInitialLength";
    public static final String ANSWER_PLACEHOLDER = "AnswerPlaceholder";
    public static final String TASK_WINDOWS = "taskWindows";
    public static final String RESOURCE_PATH = "resourcePath";
    public static final String COURSE_DIRECTORY = "courseDirectory";
    public static final String SUBTASK_INFO = "AnswerPlaceholderSubtaskInfo";
    public static final String SUBTASK_INFOS = "subtaskInfos";
    public static final String ADDITIONAL_HINTS = "additionalHints";
    public static final String POSSIBLE_ANSWER = "possibleAnswer";
    public static final String SELECTED = "selected";
    public static final String TASK_TEXT = "taskText";
    public static final String PLACEHOLDER_TEXT = "placeholderText";
    public static final String LAST_SUBTASK_INDEX = "lastSubtaskIndex";
    public static final String THEORY_TAG = "theoryTask";
    public static final String ADAPTIVE_TASK_PARAMETERS = "adaptiveTaskParameters";
    public static final String ADAPTIVE = "adaptive";
    public static final String PYCHARM_TASK = "PyCharmTask";
    public static final String EDU_TASK = "EduTask";
    public static final String TASK_WITH_SUBTASKS = "TaskWithSubtasks";
    public static final String THEORY_TASK = "TheoryTask";
    public static final String CHOICE_TASK = "ChoiceTask";
    public static final String CODE_TASK = "CodeTask";
    public static final String TASK_TEXTS = "taskTexts";

    private Xml() {
    }

    public static int getVersion(Element element) throws StudyUnrecognizedFormatException {
      if (element.getChild(COURSE_ELEMENT) != null) {
        return 1;
      }

      final Element taskManager = element.getChild(MAIN_ELEMENT);

      Element versionElement = getChildWithName(taskManager, "VERSION");
      if (versionElement == null) {
        return -1;
      }

      return Integer.valueOf(versionElement.getAttributeValue(VALUE));
    }

    @NotNull
    public static Element convertToSecondVersion(@NotNull Project project,
                                                 @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToSecondVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToThirdVersion(@NotNull Project project,
                                                @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToThirdVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToFourthVersion(@NotNull Project project,
                                                 @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToFourthVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToFifthVersion(@NotNull Project project,
                                                @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToFifthVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToSixthVersion(@NotNull Project project,
                                                @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToSixthVersionXmlConverter().convert(project, element);
    }

    @NotNull
    public static Element convertToSeventhVersion(@NotNull Project project,
                                                  @NotNull Element element) throws StudyUnrecognizedFormatException {
      return new ToSeventhVersionXmlConverter().convert(project, element);
    }

    @Nullable
    public static VirtualFile getTaskDir(@NotNull Project project, @NotNull Element lesson, @NotNull Element task)
      throws StudyUnrecognizedFormatException {
      final VirtualFile lessonDir = project.getBaseDir().findChild(EduNames.LESSON + getAsInt(lesson, INDEX));
      if (lessonDir == null) return null;
      VirtualFile taskDir = lessonDir.findChild(EduNames.TASK + getAsInt(task, INDEX));
      if (taskDir == null) {
        return null;
      }
      VirtualFile srcDir = taskDir.findChild(EduNames.SRC);
      if (srcDir != null) {
        taskDir = srcDir;
      }
      return taskDir;
    }

    public static String addStatus(XMLOutputter outputter,
                                   Map<String, String> placeholderTextToStatus,
                                   String taskStatus,
                                   Element placeholder) {
      String placeholderText = outputter.outputString(placeholder);
      String status = placeholderTextToStatus.get(placeholderText);
      if (status != null) {
        addChildWithName(placeholder, STATUS, status);
        if (taskStatus == null || status.equals(CheckStatus.Failed.toString())) {
          taskStatus = status;
        }
      }
      return taskStatus;
    }

    public static void addInitialState(Document document, Element placeholder) throws StudyUnrecognizedFormatException {
      Element initialState = getChildWithName(placeholder, INITIAL_STATE).getChild(MY_INITIAL_STATE);
      int initialLine = getAsInt(initialState, MY_LINE);
      int initialStart = getAsInt(initialState, MY_START);
      int initialOffset = document.getLineStartOffset(initialLine) + initialStart;
      addChildWithName(initialState, OFFSET, initialOffset);
      renameElement(getChildWithName(initialState, MY_LENGTH), LENGTH);
    }

    public static void addOffset(Document document, Element placeholder) throws StudyUnrecognizedFormatException {
      int line = getAsInt(placeholder, LINE);
      int start = getAsInt(placeholder, START);
      int offset = document.getLineStartOffset(line) + start;
      addChildWithName(placeholder, OFFSET, offset);
    }

    public static int getAsInt(Element element, String name) throws StudyUnrecognizedFormatException {
      return Integer.valueOf(getChildWithName(element, name).getAttributeValue(VALUE));
    }

    public static void incrementIndex(Element element) throws StudyUnrecognizedFormatException {
      Element index = getChildWithName(element, INDEX);
      int indexValue = Integer.parseInt(index.getAttributeValue(VALUE));
      changeValue(index, indexValue + 1);
    }

    public static void renameElement(Element element, String newName) {
      element.setAttribute(NAME, newName);
    }

    public static void changeValue(Element element, Object newValue) {
      element.setAttribute(VALUE, newValue.toString());
    }

    public static Element addChildWithName(Element parent, String name, Element value) {
      Element child = new Element(OPTION);
      child.setAttribute(NAME, name);
      child.addContent(value);
      parent.addContent(child);
      return value;
    }

    public static Element addChildWithName(Element parent, String name, Object value) {
      Element child = new Element(OPTION);
      child.setAttribute(NAME, name);
      child.setAttribute(VALUE, value.toString());
      parent.addContent(child);
      return child;
    }

    public static Element addChildList(Element parent, String name, List<Element> elements) {
      Element listElement = new Element(LIST);
      for (Element element : elements) {
        listElement.addContent(element);
      }
      return addChildWithName(parent, name, listElement);
    }

    public static Element addChildMap(Element parent, String name, Map<String, Element> value) {
      Element mapElement = new Element(MAP);
      for (Map.Entry<String, Element> entry : value.entrySet()) {
        Element entryElement = new Element("entry");
        mapElement.addContent(entryElement);
        String key = entry.getKey();
        entryElement.setAttribute("key", key);
        Element valueElement = new Element("value");
        valueElement.addContent(entry.getValue());
        entryElement.addContent(valueElement);
      }
      return addChildWithName(parent, name, mapElement);
    }

    public static Element addTextChildMap(Element parent, String name, Map<String, String> value) {
      Element mapElement = new Element(MAP);
      for (Map.Entry<String, String> entry : value.entrySet()) {
        Element entryElement = new Element("entry");
        mapElement.addContent(entryElement);
        String key = entry.getKey();
        entryElement.setAttribute("key", key);
        entryElement.setAttribute("value", entry.getValue());
      }
      return addChildWithName(parent, name, mapElement);
    }

    public static List<Element> getChildList(Element parent, String name) throws StudyUnrecognizedFormatException {
      return getChildList(parent, name, false);
    }

    public static List<Element> getChildList(Element parent, String name, boolean optional) throws StudyUnrecognizedFormatException {
      Element listParent = getChildWithName(parent, name, optional);
      if (listParent != null) {
        Element list = listParent.getChild(LIST);
        if (list != null) {
          return list.getChildren();
        }
      }
      return Collections.emptyList();
    }

    public static Element getChildWithName(Element parent, String name) throws StudyUnrecognizedFormatException {
      return getChildWithName(parent, name, false);
    }

    public static Element getChildWithName(Element parent, String name, boolean optional) throws StudyUnrecognizedFormatException {
      for (Element child : parent.getChildren()) {
        Attribute attribute = child.getAttribute(NAME);
        if (attribute == null) {
          continue;
        }
        if (name.equals(attribute.getValue())) {
          return child;
        }
      }
      if (optional) {
        return null;
      }
      throw new StudyUnrecognizedFormatException();
    }

    public static <K, V> Map<K, V> getChildMap(Element element, String name) throws StudyUnrecognizedFormatException {
      return getChildMap(element, name, false);
    }

    public static <K, V> Map<K, V> getChildMap(Element element, String name, boolean optional) throws StudyUnrecognizedFormatException {
      Element mapParent = getChildWithName(element, name, optional);
      if (mapParent != null) {
        Element map = mapParent.getChild(MAP);
        if (map != null) {
          HashMap result = new HashMap();
          for (Element entry : map.getChildren()) {
            Object key = entry.getAttribute(KEY) == null ? entry.getChild(KEY).getChildren().get(0) : entry.getAttributeValue(KEY);
            Object value = entry.getAttribute(VALUE) == null ? entry.getChild(VALUE).getChildren().get(0) : entry.getAttributeValue(VALUE);
            result.put(key, value);
          }
          return result;
        }
      }
      return Collections.emptyMap();
    }

    /**
     * Suitable for course xml with version 5 or above
     *
     * @param taskManagerElement element with {@link MAIN_ELEMENT} tag
     * @return course element
     * @throws StudyUnrecognizedFormatException if course element can not be found
     */
    @NotNull
    public static Element getCourseElement(@NotNull Element taskManagerElement) throws StudyUnrecognizedFormatException {
      Element courseHolder = getChildWithName(taskManagerElement, COURSE);
      Element courseElement = courseHolder.getChild(COURSE_TITLED);
      if (courseElement == null) {
        courseElement = courseHolder.getChild(REMOTE_COURSE);
        if (courseElement == null) {
          throw new StudyUnrecognizedFormatException();
        }
      }
      return courseElement;
    }
  }

  public static class Json {

    public static final String TASK_LIST = "task_list";
    public static final String TASK_FILES = "task_files";
    public static final String FILES = "files";
    public static final String TESTS = "test";
    public static final String TEXTS = "text";
    public static final String HINTS = "hints";
    public static final String SUBTASK_INFOS = "subtask_infos";
    public static final String FORMAT_VERSION = "format_version";
    public static final String INDEX = "index";
    public static final String TASK_TYPE = "task_type";
    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String LAST_SUBTASK = "last_subtask_index";

    private Json() {
    }

    public static void removeIndexFromSubtaskInfos(JsonObject placeholderObject) {
      JsonArray infos = placeholderObject.getAsJsonArray(SUBTASK_INFOS);
      Map<Integer, JsonObject> objectsToInsert = new HashMap<>();
      for (JsonElement info : infos) {
        JsonObject object = info.getAsJsonObject();
        int index = object.getAsJsonPrimitive(INDEX).getAsInt();
        objectsToInsert.put(index, object);
      }
      placeholderObject.remove(SUBTASK_INFOS);
      JsonObject newInfos = new JsonObject();
      placeholderObject.add(SUBTASK_INFOS, newInfos);
      for (Map.Entry<Integer, JsonObject> entry : objectsToInsert.entrySet()) {
        newInfos.add(entry.getKey().toString(), entry.getValue());
      }
    }

    public static class LessonAdapter implements JsonDeserializer<Lesson> {
      @Override
      public Lesson deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Task.class, new TaskAdapter()).create();
        final Lesson lesson = gson.fromJson(json, Lesson.class);
        final String name = lesson.getName();
        if (StepikNames.PYCHARM_ADDITIONAL.equals(name)) {
          lesson.setName(EduNames.ADDITIONAL_MATERIALS);
        }
        return lesson;
      }
    }

    public static class TaskAdapter implements JsonSerializer<Task>, JsonDeserializer<Task> {

      @Override
      public JsonElement serialize(Task src, Type typeOfSrc, JsonSerializationContext context) {
        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
        return serializeWithTaskType(src, gson);
      }

      @Override
      public Task deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
        return doDeserialize(json, gson);
      }
    }

    @NotNull
    public static JsonObject serializeWithTaskType(Task src, Gson gson) {
      JsonElement tree = gson.toJsonTree(src);
      final JsonObject task = tree.getAsJsonObject();
      task.add(TASK_TYPE, new JsonPrimitive(src.getTaskType()));
      return task;
    }

    @Nullable
    public static Task doDeserialize(JsonElement json, Gson gson) {
      final JsonObject object = json.getAsJsonObject();
      if (object.has(NAME) && StepikNames.PYCHARM_ADDITIONAL.equals(object.get(NAME).getAsString())) {
        object.remove(NAME);
        object.add(NAME, new JsonPrimitive(EduNames.ADDITIONAL_MATERIALS));
      }
      if (object.has(TASK_TYPE)) {
        final String taskType = object.get(TASK_TYPE).getAsString();
        switch (taskType) {
          case "choice": return gson.fromJson(object, ChoiceTask.class);
          case "theory": return gson.fromJson(object, TheoryTask.class);
          case "code": return gson.fromJson(object, CodeTask.class);
          case "edu": return gson.fromJson(object, EduTask.class);
          case "subtasks": return gson.fromJson(object, TaskWithSubtasks.class);
          case "output": return gson.fromJson(object, OutputTask.class);
          case "pycharm": return gson.fromJson(object, EduTask.class);     // deprecated: old courses have pycharm tasks
          default: {
            LOG.warn("Unsupported task type " + taskType);
            return null;
          }
        }
      }
      LOG.warn("No task type found in json " + json.toString());
      return null;
    }
  }
}
