/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

public interface IEdgeDefinition
{
    public static final String DEFAULT_CHANNEL = null;

    public enum RequiredConditions
    {
        ALL,
        ONE
    }

    INodeDefinition getSource();

    INodeDefinition getDestination();

    String getRegExp();

    Boolean getTriggerAlways();

    String getCheckExp();

    String getOnWalk();

    Boolean getTriggerOnEof();

    RequiredConditions getRequiredConditions();

    ITimeInterval getTimeIntervalSinceLastMicrotransition();

    ITimeInterval getTimeIntervalSinceLastTransition();

    ITimeInterval getTimeIntervalSinceAutomatonStart();

    ITimeInterval getTimeIntervalForEvent();

    String getChannel();

    String getComment();
}
