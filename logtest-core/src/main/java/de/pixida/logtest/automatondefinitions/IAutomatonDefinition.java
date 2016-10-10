/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.List;

public interface IAutomatonDefinition
{
    void load();

    List<? extends INodeDefinition> getNodes();

    List<? extends IEdgeDefinition> getEdges();

    String getOnLoad();

    String getDisplayName();

    String getComment();

    String getScriptLanguage();
}
