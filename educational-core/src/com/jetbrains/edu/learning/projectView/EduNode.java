package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.JBColor;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public interface EduNode {
  JBColor LIGHT_GREEN = new JBColor(new Color(0, 134, 0), new Color(98, 150, 85));

  @Nullable
  AbstractTreeNode modifyChildNode(AbstractTreeNode childNode);

  PsiDirectoryNode createChildDirectoryNode(StudyItem item, PsiDirectory value);

}
