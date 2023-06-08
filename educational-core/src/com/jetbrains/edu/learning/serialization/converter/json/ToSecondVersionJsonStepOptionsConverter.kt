package com.jetbrains.edu.learning.serialization.converter.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*;

public class ToSecondVersionJsonStepOptionsConverter implements JsonStepOptionsConverter {

  @NotNull
  @Override
  public ObjectNode convert(@NotNull ObjectNode stepOptionsJson) {
    final JsonNode files = stepOptionsJson.get(FILES);
    if (files != null) {
      for (JsonNode taskFileElement : files) {
        JsonNode placeholders = taskFileElement.get(PLACEHOLDERS);
        for (JsonNode placeholder : placeholders) {
          convertToAbsoluteOffset(taskFileElement, (ObjectNode)placeholder);
          convertMultipleHints((ObjectNode)placeholder);
          convertToSubtaskInfo((ObjectNode)placeholder);
        }
      }
    }
    return stepOptionsJson;
  }

  private static void convertToAbsoluteOffset(@NotNull JsonNode taskFileObject, @NotNull ObjectNode placeholderObject) {
    int line = placeholderObject.get(LINE).asInt();
    int start = placeholderObject.get(START).asInt();
    if (line == -1) {
      placeholderObject.put(OFFSET, start);
    }
    else {
      Document document = EditorFactory.getInstance().createDocument(taskFileObject.get(TEXT).asText());
      placeholderObject.put(OFFSET, document.getLineStartOffset(line) + start);
    }
  }

  private static void convertMultipleHints(@NotNull ObjectNode placeholderObject) {
    final String hintString = placeholderObject.get(HINT).asText();
    final ArrayNode hintsArray = placeholderObject.putArray(ADDITIONAL_HINTS);
    try {
      final List<String> hints = new ObjectMapper().readValue(hintString, new TypeReference<List<String>>() {});
      if (hints != null && !hints.isEmpty()) {
        for (int i = 0; i < hints.size(); i++) {
          if (i == 0) {
            placeholderObject.put(HINT, hints.get(0));
            continue;
          }
          hintsArray.add(hints.get(i));
        }
      }
      else {
        placeholderObject.put(HINT, "");
      }
    }
    catch (JsonMappingException e) {
      hintsArray.add(hintString);
    }
    catch (IOException e) {
      hintsArray.add(hintString);
    }
  }

  private static void convertToSubtaskInfo(@NotNull ObjectNode placeholderObject) {
    ObjectNode subtaskInfo = new ObjectMapper().createObjectNode();
    final ArrayNode subtaskInfos = placeholderObject.putArray(SUBTASK_INFOS);
    final ArrayNode hintsArray = subtaskInfo.putArray(HINTS);

    hintsArray.add(placeholderObject.get(HINT).asText());
    JsonNode additionalHints = placeholderObject.get(ADDITIONAL_HINTS);
    if (additionalHints != null) {
      for (JsonNode hint : additionalHints) {
        hintsArray.add(hint);
      }
    }

    subtaskInfos.add(subtaskInfo);
    subtaskInfo.put(INDEX, 0);
    subtaskInfo.put(POSSIBLE_ANSWER, placeholderObject.get(POSSIBLE_ANSWER).asText());
  }
}
