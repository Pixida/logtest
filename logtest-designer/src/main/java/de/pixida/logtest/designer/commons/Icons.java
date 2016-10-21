/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.commons;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public abstract class Icons
{
    public static ImageView getIconGraphics(final String name)
    {
        return new ImageView(getIconImage(name));
    }

    public static Image getIconImage(final String name)
    {
        return new Image(Icons.class.getResourceAsStream("/icons/" + name + ".png"));
    }
}
