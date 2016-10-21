/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application
{
    public Main()
    {
        // Empty constructor needed by checkstyle
    }

    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage)
    {
        new MainWindow(primaryStage);
    }
}
