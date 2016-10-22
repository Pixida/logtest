/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.commons;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public abstract class SelectFileButton
{
    private static File lastFolder = null;

    public static Button createButtonWithFileSelection(final TextField inputFieldShowingPath, final String icon, final String title,
        final String fileMask, final String fileMaskDescription)
    {
        final Button selectLogFileButton = new Button("Select");
        selectLogFileButton.setGraphic(Icons.getIconGraphics(icon));
        selectLogFileButton.setOnAction(event -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            if (StringUtils.isNotBlank(fileMask) && StringUtils.isNotBlank(fileMaskDescription))
            {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(fileMaskDescription, fileMask));
            }
            File initialDirectory = new File(inputFieldShowingPath.getText().trim());
            if (initialDirectory == null || !initialDirectory.isDirectory())
            {
                initialDirectory = initialDirectory.getParentFile();
            }
            if (initialDirectory == null || !initialDirectory.isDirectory())
            {
                if (lastFolder != null)
                {
                    initialDirectory = lastFolder;
                }
                else
                {
                    initialDirectory = new File(".");
                }
            }
            fileChooser.setInitialDirectory(initialDirectory);
            final File selectedFile = fileChooser.showOpenDialog(((Node) event.getTarget()).getScene().getWindow());
            if (selectedFile != null)
            {
                inputFieldShowingPath.setText(selectedFile.getAbsolutePath());
                final File parent = selectedFile.getParentFile();
                if (parent != null && parent.isDirectory())
                {
                    lastFolder = parent;
                }
            }
        });
        return selectLogFileButton;
    }

}
