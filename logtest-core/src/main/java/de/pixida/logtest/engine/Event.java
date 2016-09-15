/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import org.apache.commons.lang3.Validate;

import de.pixida.logtest.logreaders.ILogEntry;

public final class Event
{
    private final ILogEntry logEntry;
    private final boolean eof;

    private Event(final ILogEntry aLogEntry)
    {
        Validate.notNull(aLogEntry);
        Validate.notNull(aLogEntry.getPayload());
        this.logEntry = aLogEntry;
        this.eof = false;
    }

    private Event(final boolean aEof)
    {
        Validate.isTrue(aEof);
        this.logEntry = null;
        this.eof = aEof;
    }

    static Event fromLogEntry(final ILogEntry value)
    {
        return new Event(value);
    }

    static Event fromEof()
    {
        return new Event(true);
    }

    boolean isEof()
    {
        return this.eof;
    }

    boolean isLogEntry()
    {
        return this.logEntry != null;
    }

    ILogEntry getLogEntry()
    {
        return this.logEntry;
    }
}
