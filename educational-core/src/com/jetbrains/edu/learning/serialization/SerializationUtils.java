package com.jetbrains.edu.learning.serialization;

import com.google.gson.*;
import com.jetbrains.edu.learning.authUtils.TokenInfo;
import org.jdom.Attribute;
import org.jdom.Element;

import java.lang.reflect.Type;

public class SerializationUtils {

  public static final String LINE = "line";
  public static final String START = "start";
  public static final String HINT = "hint";
  public static final String ADDITIONAL_HINTS = "additional_hints";
  public static final String OFFSET = "offset";
  public static final String LESSONS = "lessons";
  public static final String ITEMS = "items";
  public static final String COURSE = "course";
  public static final String ID = "id";
  public static final String STATUS = "status";
  public static final String AUTHOR = "author";
  public static final String AUTHORS = "authors";
  public static final String SUBTASK_MARKER = "_subtask";
  public static final String ENVIRONMENT = "environment";

  private SerializationUtils() {
  }

  public static class Xml {
    public static final String SETTINGS_NAME = "EduSettings";
    public static final String OPTION = "option";
    public static final String NAME = "name";
    public static final String USER = "user";
    public static final String STEPIK_USER = "StepikUser";

    private Xml() {
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
  }

  public static class Json {

    public static final String TASK_LIST = "task_list";
    public static final String TASK_FILES = "task_files";
    public static final String TASK_TEXTS = "task_texts";
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
    public static final String ITEMS = "items";
    public static final String ITEM_TYPE = "type";
    public static final String FRAMEWORK_TYPE = "framework";
    public static final String PLACEHOLDERS = "placeholders";
    public static final String POSSIBLE_ANSWER = "possible_answer";
    public static final String PLACEHOLDER_TEXT = "placeholder_text";
    public static final String SELECTED = "selected";
    public static final String FILE_WRAPPER_TEXT = "text";
    public static final String DESCRIPTION_TEXT = "description_text";
    public static final String DESCRIPTION_FORMAT = "description_format";
    public static final String ADDITIONAL_FILES = "additional_files";
    public static final String TEXT = "text";
    public static final String IS_VISIBLE = "is_visible";
    public static final String DEPENDENCY = "dependency";
    public static final String DEPENDENCY_FILE = "file";
    public static final String TEST_FILES = "test_files";
    public static final String VERSION = "version";
    public static final String COURSE_TYPE = "course_type";
    public static final String PROGRAMMING_LANGUAGE = "programming_language";
    public static final String EDU_TASK = "edu_task";
    public static final String TASK = "task";

    private Json() {
    }
  }

  public static class TokenInfoDeserializer implements JsonDeserializer<TokenInfo> {

    @Override
    public TokenInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      final JsonObject jsonObject = json.getAsJsonObject();

      final String accessToken = jsonObject.get("access_token").getAsString();
      // we don't have refresh token in tests
      final String refreshToken = jsonObject.get("refresh_token") == null ? "" : jsonObject.get("refresh_token").getAsString();
      final long expiresIn = jsonObject.get("expires_in").getAsLong();
      final long expiringTime = expiresIn + (System.currentTimeMillis() / 1000);

      TokenInfo tokenInfo = new TokenInfo();
      tokenInfo.setRefreshToken(refreshToken);
      tokenInfo.setAccessToken(accessToken);
      tokenInfo.setExpiresIn(expiringTime);
      return tokenInfo;
    }
  }
}
