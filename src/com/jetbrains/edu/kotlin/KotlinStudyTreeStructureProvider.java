package com.jetbrains.edu.kotlin;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.projectView.StudyTreeStructureProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class KotlinStudyTreeStructureProvider extends StudyTreeStructureProvider {
    private static final String UTIL_DIR = "util";
    private static final String OUT_DIR = "out";
    @NotNull
    @Override
    public Collection<AbstractTreeNode> modify(@NotNull AbstractTreeNode parent,
                                               @NotNull Collection<AbstractTreeNode> children,
                                               ViewSettings settings) {

        Collection<AbstractTreeNode> old_nodes = super.modify(parent, children, settings);
        Project project = parent.getProject();
        if (project == null) {
            return old_nodes;
        }
        if (StudyTaskManager.getInstance(project).getCourse() == null) {
            return old_nodes;
        }
        Collection<AbstractTreeNode> nodes = new ArrayList<AbstractTreeNode>();
        for (AbstractTreeNode node: old_nodes) {
            if (node.getValue() instanceof PsiDirectory) {
                String name = ((PsiDirectory)node.getValue()).getName();
                if (!name.equals(UTIL_DIR) && !name.equals(OUT_DIR)) {
                    nodes.add(node);
                }
            } else {
                nodes.add(node);
            }
        }
        return nodes;
    }
}
