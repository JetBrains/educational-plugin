package com.jetbrains.edu.learning.serialization.converter.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*;

public class ToThirdVersionJsonStepOptionsConverter implements JsonStepOptionsConverter {

  @NotNull
  @Override
  public JsonObject convert(@NotNull JsonObject stepOptionsJson) {
    if (!stepOptionsJson.has(LAST_SUBTASK)) return stepOptionsJson;
    final int lastSubtaskIndex = stepOptionsJson.get(LAST_SUBTASK).getAsInt();
    if (lastSubtaskIndex == 0) return stepOptionsJson;
    final JsonArray tests = stepOptionsJson.getAsJsonArray(TESTS);
    if (tests.size() > 0) {
      final JsonObject fileWrapper = tests.get(0).getAsJsonObject();
      if (fileWrapper.has(NAME)) {
        replaceWithSubtask(fileWrapper);
      }
    }
    final JsonArray descriptions = stepOptionsJson.getAsJsonArray(TEXTS);
    if (descriptions != null && descriptions.size() > 0) {
      final JsonObject fileWrapper = descriptions.get(0).getAsJsonObject();
      if (fileWrapper.has(NAME)) {
        replaceWithSubtask(fileWrapper);
      }
    }
    return stepOptionsJson;
  }

  private static void replaceWithSubtask(@NotNull JsonObject fileWrapper) {
    final String file = fileWrapper.get(NAME).getAsString();
    final String extension = FileUtilRt.getExtension(file);
    final String name = FileUtil.getNameWithoutExtension(file);
    if (!name.contains(SerializationUtils.SUBTASK_MARKER)) {
      fileWrapper.remove(NAME);
      fileWrapper.add(NAME, new JsonPrimitive(name + "_subtask0." + extension));
    }
  }
}
