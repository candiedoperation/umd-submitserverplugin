<img src="https://github.com/candiedoperation/umd-submitserverplugin/blob/4b5d951461c376c711019ebf6ad2bdf5396c1f58/src/main/resources/META-INF/pluginIcon.svg" width=180 height=180 />
<h1>UMD CS Submit Server Plugin</h1>
<p>The University of Maryland's submit server plugin, completely rewritten for the IntelliJ IDEA IDE. The Latest Version of the plugin can be found on this repository's releases section. Future version might be available on the IDEA Plugin Marketplace.</p>

# Installation Instructions

- [Download the plugin file](https://github.com/candiedoperation/umd-submitserverplugin/releases/download/v1.0.1/umd-submitserverplugin-1.0.1.zip) from this release
- Head to the Plugins section in IDEA IDE. (You could search for Plugins after using Ctrl+Shift+A or Help > Actions)
- Near the Installed plugins tab, there exists a settings icon. Clicking on it lets you select **Install plugin from Disk**
- Browse for the ZIP file you downloaded from this release and proceed with the installation

# Using the Plugin

- The plugin is automatically enabled when a UMD CS project is detected
- The Tools Menu has a section named University of Maryland CS
- Once that option's clicked, you'll find a button to Submit the current project
- Enter you credentials and you'll receive a confirmation when your project is submitted

# Importing a UMD CS Project into IntelliJ IDEA
⚠️ This following method is beta and supports only one dependency, JUnit4. Other dependencies will need to be fixed manually. Otherwise, use the native IntelliJ Eclipse import wizard.

## Using this plugin's Import feature

- Press Ctrl+Shift+A anywhere in the IDE
- Search for Import UMD CS Project
- Select the option, select the downloaded ZIP file and select a destination

## Using the IntelliJ IDEA Eclipse Import Wizard

- Delete the project if you already imported it
- Unzip the ZIP file to a destination folder
- Close all IntelliJ projects, get to home screen
- Press Ctrl+Shift+A and search for *Import project from Existing Sources*
- Select the option, select the unzipped folder
- The external import source would be the Eclipse option. Follow the other steps in the wizard

<br/>

© Copyright 2024  Atheesh Thirumalairajan  
This project is not affiliated with the University of Maryland
