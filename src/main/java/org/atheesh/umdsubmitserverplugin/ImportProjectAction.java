package org.atheesh.umdsubmitserverplugin;

import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.rt.coverage.data.ProjectData;
import net.lingala.zip4j.ZipFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ImportProjectAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /* Project Might be Null */
        Project project = e.getProject();

        /* Define Required Paths */
        String srcImportPath = "";
        String dstImportPath = "";

        // Show file chooser for selecting Eclipse project file
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        fileChooserDescriptor.setTitle("Import a UMD CS Project");
        fileChooserDescriptor.setDescription("Choose your project's downloaded ZIP file");

        /* Add File Chooser Filter Properties */
        fileChooserDescriptor.withFileFilter(
                file -> file.getName().toLowerCase().endsWith(".zip") &&
                        (!file.isDirectory())
        );

        /* Open the File Chooser Dialog */
        VirtualFile selectedFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);

        if (selectedFile != null) {
            if (!selectedFile.isDirectory() && new ZipFile(selectedFile.getPath()).isValidZipFile()) {
                /* Update Selected Source Path */
                srcImportPath = selectedFile.getPath();

                /* Open Destination Choose Dialog */
                ImportProjectDstDialog dstDialog = new ImportProjectDstDialog(project);
                dstDialog.show();

                /* Returns Empty String if Cancelled */
                if (!dstDialog.getDstPath().isBlank()) {
                    /* Update Selected Destination Path */
                    dstImportPath = dstDialog.getDstPath();

                    /* Start the Import Process */
                    ImportProjectCore.startImport(srcImportPath, dstImportPath);
                }
            } else {
                /* Show Error */
                Messages.showErrorDialog(
                        "Please select a valid ZIP file. The selected file is either invalid or corrupted.",
                        "Invalid CS Project File Selected"
                );

                /* Select The File Again */
                this.actionPerformed(e);
            }
        }
    }
}
