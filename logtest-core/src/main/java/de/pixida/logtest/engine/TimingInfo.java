/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class TimingInfo
{
    private long startTime;
    private long timeOfLastMicrotransition;
    private long timeOfLastTransition;
    private long timeOfCurrentEvent;

    public TimingInfo()
    {
        // Empty constructor needed by checkstyle
    }

    public long getStartTime()
    {
        return this.startTime;
    }

    public void setStartTime(final long value)
    {
        this.startTime = value;
    }

    public long getTimeOfLastMicrotransition()
    {
        return this.timeOfLastMicrotransition;
    }

    public void setTimeOfLastMicrotransition(final long value)
    {
        this.timeOfLastMicrotransition = value;
    }

    public long getTimeOfLastTransition()
    {
        return this.timeOfLastTransition;
    }

    public void setTimeOfLastTransition(final long value)
    {
        this.timeOfLastTransition = value;
    }

    public long getTimeOfCurrentEvent()
    {
        return this.timeOfCurrentEvent;
    }

    public void setTimeOfCurrentEvent(final long value)
    {
        this.timeOfCurrentEvent = value;
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
