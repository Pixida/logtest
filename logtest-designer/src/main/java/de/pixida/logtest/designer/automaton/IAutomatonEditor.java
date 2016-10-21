/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import javafx.scene.Node;

interface IAutomatonEditor
{
    void handleChange();

    void handleMinorChange();

    void showConfigFrame(Node configFrame);

    void showDefaultConfigFrame();
}
