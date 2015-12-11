package com.jetbrains.edu.kotlin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkListCellRenderer;
import com.intellij.openapi.projectRoots.ui.ProjectJdksEditor;
import com.intellij.ui.ComboboxWithBrowseButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class KotlinSdkComboBox extends ComboboxWithBrowseButton {
    public KotlinSdkComboBox() {
        getComboBox().setRenderer(new SdkListCellRenderer("<No Interpreter>", true));
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Sdk selectedSdk = getSelectedSdk();
                final Project project = ProjectManager.getInstance().getDefaultProject();
                ProjectJdksEditor editor = new ProjectJdksEditor(selectedSdk, project, KotlinSdkComboBox.this);
                if (editor.showAndGet()) {
                    selectedSdk = editor.getSelectedJdk();
                    updateSdkList(selectedSdk, false);
                }
            }
        });
        updateSdkList(null, true);
    }

    public void updateSdkList(Sdk sdkToSelect, boolean selectAnySdk) {
        final List<Sdk> sdkList = ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance());
        if (selectAnySdk && sdkList.size() > 0) {
            sdkToSelect = sdkList.get(0);
        }
        sdkList.add(0, null);
        getComboBox().setModel(new DefaultComboBoxModel<Sdk>(sdkList.toArray(new Sdk[sdkList.size()])));
        getComboBox().setSelectedItem(sdkToSelect);
    }

    public Sdk getSelectedSdk() {
        return (Sdk) getComboBox().getSelectedItem();
    }
}
