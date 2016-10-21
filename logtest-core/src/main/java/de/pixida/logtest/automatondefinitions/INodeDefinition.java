/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

public interface INodeDefinition
{
    public enum Flag
    {
        IS_INITIAL,
        IS_SUCCESS,
        IS_FAILURE
    }

    public enum Type
    {
        INITIAL,
        SUCCESS,
        FAILURE
    }

    String getName();

    String getDescription();

    String getId();

    Type getType();

    String getOnEnter();

    String getOnLeave();

    String getSuccessCheckExp();

    boolean getWait();
}
