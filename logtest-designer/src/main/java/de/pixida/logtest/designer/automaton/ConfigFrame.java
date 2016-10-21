/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import org.apache.commons.lang3.Validate;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

class ConfigFrame extends ScrollPane
{
    private final String title;
    private final VBox vbox = new VBox();

    ConfigFrame(final String aTitle)
    {
        Validate.notNull(aTitle);
        this.title = aTitle;

        this.setHbarPolicy(ScrollBarPolicy.NEVER);
        this.setFitToWidth(true);

        final int insets = 10;
        this.vbox.setPadding(new Insets(insets));
        final int spacing = 8;
        this.vbox.setSpacing(spacing);

        final Text titleText = new Text(this.title);
        final int titleFontSize = 14;
        titleText.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, titleFontSize));
        this.vbox.getChildren().add(titleText);

        this.setContent(this.vbox);

        final int minWidth = 200;
        this.setMinWidth(minWidth);
    }

    void addOption(final String name, final Node control)
    {
        final Text nameText = new Text(name + ":");
        this.vbox.getChildren().add(nameText);

        final int marginLeft = 8;
        VBox.setMargin(control, new Insets(0, 0, 0, marginLeft));
        this.vbox.getChildren().add(control);
    }
}
