/*
    University of Maryland CS Submit Server Plugin
    Copyright (C) 2024  Atheesh Thirumalairajan

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package org.atheesh.umdsubmitserverplugin;

import com.intellij.ide.RecentProjectsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImportProjectCore {
    public static void startImport(String srcPath, String dstPath) {
        try {
            /* Import the ZIP File */
            ZipFile projectArchive = new ZipFile(srcPath);

            /* Check if the project already exists in Destination */
            FileHeader fileHeader = projectArchive.getFileHeaders().get(0);
            String rootFolder = fileHeader.getFileName().split("/")[0];
            if (Files.exists(Path.of(dstPath + "/" + rootFolder)))
                throw new ImportProjectException("Project already exists at Destination");

            /* Unzip the Source ZIP file to Destination */
            projectArchive.extractAll(dstPath);

            /* Process */
            importEclipseProject(dstPath + "/" + rootFolder, rootFolder);

            /* Successful Project Import */
            RecentProjectsManager.getInstance().setLastProjectCreationLocation(dstPath);
        } catch (ZipException ex) {
            Messages.showErrorDialog(
                    "Failed to extract Project Archive: " + ex.getMessage(),
                    "Project Import Failed"
            );
        } catch (ImportProjectException ex) {
            Messages.showErrorDialog(
                    ex.getMessage(),
                    "Project Import Failed"
            );
        }
    }

    private static void importEclipseProject(String projectRoot, String projectName) throws ImportProjectException {
        try {
            /* Access the Project Root Directory, Open Project */
            Project project = ProjectManager.getInstance().loadAndOpenProject(projectRoot);

            /* Set the Project SDK */
            ApplicationManager.getApplication().runWriteAction(() -> {
                Sdk[] sdkTable = ProjectJdkTable.getInstance().getAllJdks();
                ProjectRootManager.getInstance(project).setProjectSdk(sdkTable[0]);
            });

            /* Generate IntelliJ .iml File */
            createModuleIML(projectRoot, projectName);

            /* Generate IntelliJ modules.xml */
            createModuleXML(projectRoot, projectName);

            /* Save Project and Reload Modules */
            project.save();
        } catch (Exception e) {
            throw new ImportProjectException(e.getMessage());
        }
    }

    public static void createModuleIML(String projectRoot, String moduleName) throws ParserConfigurationException, TransformerException {
        // Create DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create Document
        Document document = builder.newDocument();

        // Create module element
        Element moduleElement = document.createElement("module");
        moduleElement.setAttribute("type", "JAVA_MODULE");
        moduleElement.setAttribute("version", "4");
        document.appendChild(moduleElement);

        // Create component element
        Element componentElement = document.createElement("component");
        componentElement.setAttribute("name", "NewModuleRootManager");
        moduleElement.appendChild(componentElement);

        // Create output element
        Element outputElement = document.createElement("output");
        outputElement.setAttribute("url", "file://$MODULE_DIR$/bin");
        componentElement.appendChild(outputElement);

        // Create exclude-output element
        Element excludeOutputElement = document.createElement("exclude-output");
        componentElement.appendChild(excludeOutputElement);

        // Create content element
        Element contentElement = document.createElement("content");
        contentElement.setAttribute("url", "file://$MODULE_DIR$");
        componentElement.appendChild(contentElement);

        // Create sourceFolder element
        Element sourceFolderElement = document.createElement("sourceFolder");
        sourceFolderElement.setAttribute("url", "file://$MODULE_DIR$/src");
        sourceFolderElement.setAttribute("isTestSource", "false");
        contentElement.appendChild(sourceFolderElement);

        // Create orderEntry elements
        Element orderEntry0 = document.createElement("orderEntry");
        orderEntry0.setAttribute("type", "inheritedJdk");
        componentElement.appendChild(orderEntry0);

        Element orderEntry1 = document.createElement("orderEntry");
        orderEntry1.setAttribute("type", "sourceFolder");
        orderEntry1.setAttribute("forTests", "false");
        componentElement.appendChild(orderEntry1);

        Element orderEntry2 = document.createElement("orderEntry");
        orderEntry2.setAttribute("type", "module-library");
        componentElement.appendChild(orderEntry2);

        Element libraryElement = document.createElement("library");
        libraryElement.setAttribute("name", "JUnit4");
        orderEntry2.appendChild(libraryElement);

        Element classesElement = document.createElement("CLASSES");
        libraryElement.appendChild(classesElement);

        Element root1Element = document.createElement("root");
        root1Element.setAttribute("url", "jar://$MAVEN_REPOSITORY$/junit/junit/4.13.1/junit-4.13.1.jar!/");
        classesElement.appendChild(root1Element);

        Element root2Element = document.createElement("root");
        root2Element.setAttribute("url", "jar://$MAVEN_REPOSITORY$/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar!/");
        classesElement.appendChild(root2Element);

        Element javadocElement = document.createElement("JAVADOC");
        libraryElement.appendChild(javadocElement);

        Element sourcesElement = document.createElement("SOURCES");
        libraryElement.appendChild(sourcesElement);

        // Output the XML to console or file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        // Create output File, Folders if not exist
        File modulesFile = new File(String.format("%s/%s.iml", projectRoot, moduleName));
        modulesFile.getParentFile().mkdirs();

        // Save Output to ProjectRoot/.idea/modules.xml
        DOMSource source = new DOMSource(document);
        StreamResult fileResult = new StreamResult(modulesFile);
        transformer.transform(source, fileResult);
    }

    private static void createModuleXML(String projectRoot, String moduleName) throws TransformerException, ParserConfigurationException {
        // Create DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create Document
        Document document = builder.newDocument();

        // Create root element
        Element projectElement = document.createElement("project");
        projectElement.setAttribute("version", "4");
        document.appendChild(projectElement);

        // Create component element
        Element componentElement = document.createElement("component");
        componentElement.setAttribute("name", "ProjectModuleManager");
        projectElement.appendChild(componentElement);

        // Create modules element
        Element modulesElement = document.createElement("modules");
        componentElement.appendChild(modulesElement);

        // Create module element
        Element moduleElement = document.createElement("module");
        moduleElement.setAttribute("fileurl", String.format("file://$PROJECT_DIR$/%s.iml", moduleName));
        moduleElement.setAttribute("filepath", String.format("$PROJECT_DIR$/%s.iml", moduleName));
        modulesElement.appendChild(moduleElement);

        // Output the XML to console or file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        // Create output File, Folders if not exist
        File modulesFile = new File(projectRoot + "/.idea/modules.xml");
        modulesFile.getParentFile().mkdirs();

        // Save Output to ProjectRoot/.idea/modules.xml
        DOMSource source = new DOMSource(document);
        StreamResult fileResult = new StreamResult(modulesFile);
        transformer.transform(source, fileResult);
    }
}
