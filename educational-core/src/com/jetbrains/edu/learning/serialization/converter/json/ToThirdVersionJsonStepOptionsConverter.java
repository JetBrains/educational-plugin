package com.jetbrains.edu.learning.serialization.converter.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*;

public class ToThirdVersionJsonStepOptionsConverter implements JsonStepOptionsConverter {

  @NotNull
  @Override
  public ObjectNode convert(@NotNull ObjectNode stepOptionsJson) {
    if (!stepOptionsJson.has(LAST_SUBTASK)) return stepOptionsJson;
    final int lastSubtaskIndex = stepOptionsJson.get(LAST_SUBTASK).asInt();
    if (lastSubtaskIndex == 0) return stepOptionsJson;
    final JsonNode tests = stepOptionsJson.get(TESTS);
    if (tests.size() > 0) {
      final JsonNode fileWrapper = tests.get(0);
      if (fileWrapper.has(NAME)) {
        replaceWithSubtask((ObjectNode)fileWrapper);
      }
    }
    final JsonNode descriptions = stepOptionsJson.get(TEXTS);
    if (descriptions != null && descriptions.size() > 0) {
      final JsonNode fileWrapper = descriptions.get(0);
      if (fileWrapper.has(NAME)) {
        replaceWithSubtask((ObjectNode)fileWrapper);
      }
    }
    return stepOptionsJson;
  }

  private static void replaceWithSubtask(@NotNull ObjectNode fileWrapper) {
    final String file = fileWrapper.get(NAME).asText();
    final String extension = FileUtilRt.getExtension(file);
    final String name = FileUtil.getNameWithoutExtension(file);
    if (!name.contains(SerializationUtils.SUBTASK_MARKER)) {
      fileWrapper.remove(NAME);
      fileWrapper.put(NAME, name + "_subtask0." + extension);
    }
  }
}
