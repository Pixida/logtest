/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.logreaders;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class GenericLogEntry implements ILogEntry
{
    private final long lineNumber;
    private final long time;
    private final String payload;
    private final String channel;

    public GenericLogEntry(final long aLineNumber, final long aTime, final String aPayload, final String aChannel)
    {
        this.lineNumber = aLineNumber;
        this.time = aTime;
        this.payload = aPayload;
        this.channel = aChannel;
    }

    public GenericLogEntry(final long aLineNumber, final long aTime, final String aPayload)
    {
        this.lineNumber = aLineNumber;
        this.time = aTime;
        this.payload = aPayload;
        this.channel = ILogEntry.DEFAULT_CHANNEL;
    }

    @Override
    public long getTime()
    {
        return this.time;
    }

    @Override
    public String getPayload()
    {
        return this.payload;
    }

    @Override
    public long getLineNumber()
    {
        return this.lineNumber;
    }

    @Override
    public String getChannel()
    {
        return this.channel;
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return ReflectionToStringBuilder.toString(this);
    }
}
