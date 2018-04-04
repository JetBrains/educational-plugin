package com.jetbrains.edu.learning;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.JBColor;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

public class AnswerPlaceholderPainter {

  //it should be the lowest highlighting layer, otherwise selection and other effects are not visible
  public static final int PLACEHOLDERS_LAYER = 0;

  private AnswerPlaceholderPainter() {

  }

  public static void drawAnswerPlaceholder(@NotNull final Editor editor, @NotNull final AnswerPlaceholder placeholder,
                                           @NotNull final JBColor color) {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    final TextAttributes textAttributes = new TextAttributes(scheme.getDefaultForeground(), scheme.getDefaultBackground(), null,
                                                             EffectType.BOXED, Font.PLAIN);
    textAttributes.setEffectColor(color);
    int startOffset = placeholder.getOffset();
    if (startOffset == -1) {
      return;
    }
    Pair<Integer, Integer> offsets = EduUtils.getPlaceholderOffsets(placeholder, editor.getDocument());
    startOffset = offsets.first;
    int endOffset = offsets.second;
    drawAnswerPlaceholder(editor, startOffset, endOffset, textAttributes, PLACEHOLDERS_LAYER);
  }

  public static void drawAnswerPlaceholder(@NotNull Editor editor,
                                           int start,
                                           int end,
                                           @Nullable TextAttributes textAttributes,
                                           int placeholdersLayer) {
    final Project project = editor.getProject();
    assert project != null;
    if (start == -1) {
      return;
    }
    RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(start, end, placeholdersLayer,
                                                                               textAttributes, HighlighterTargetArea.EXACT_RANGE);
    highlighter.setGreedyToLeft(true);
    highlighter.setGreedyToRight(true);
  }

  public static void createGuardedBlock(Editor editor, List<RangeMarker> blocks, int start, int end) {
    RangeHighlighter rh = editor.getMarkupModel()
      .addRangeHighlighter(start, end, PLACEHOLDERS_LAYER, null, HighlighterTargetArea.EXACT_RANGE);
    blocks.add(rh);
  }


  public static void createGuardedBlocks(@NotNull final Editor editor, TaskFile taskFile) {
    for (AnswerPlaceholder answerPlaceholder : taskFile.getAnswerPlaceholders()) {
      createGuardedBlocks(editor, answerPlaceholder);
    }
  }

  public static void createGuardedBlocks(@NotNull final Editor editor, AnswerPlaceholder placeholder) {
    Document document = editor.getDocument();
    if (document instanceof DocumentImpl) {
      DocumentImpl documentImpl = (DocumentImpl)document;
      List<RangeMarker> blocks = documentImpl.getGuardedBlocks();
      Pair<Integer, Integer> offsets = EduUtils.getPlaceholderOffsets(placeholder, editor.getDocument());
      Integer start = offsets.first;
      Integer end = offsets.second;
      if (start != 0) {
        createGuardedBlock(editor, blocks, start - 1, start);
      }
      if (end != document.getTextLength()) {
        createGuardedBlock(editor, blocks, end, end + 1);
      }
    }
  }
}
