/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.Set;

public interface INodeDefinition
{
    public enum Flag
    {
        IS_INITIAL,
        IS_SUCCESS,
        IS_FAILURE
    }

    Set<Flag> getFlags();

    String getOnEnter();

    String getOnLeave();

    String getSuccessCheckExp();

    boolean getWait();

    String getComment();
}
