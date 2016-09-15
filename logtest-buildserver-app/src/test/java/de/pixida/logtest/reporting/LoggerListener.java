/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.reporting;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

class LoggerListener extends AppenderSkeleton
{
    private final List<Pair<Level, String>> logEntries = new ArrayList<>();

    LoggerListener()
    {
        // Empty constructor needed by checkstyle
    }

    @Override
    public void close()
    {
    }

    @Override
    public boolean requiresLayout()
    {
        return false;
    }

    @Override
    protected void append(final LoggingEvent event)
    {
        this.logEntries.add(Pair.of(event.getLevel(), event.getMessage().toString()));
    }

    List<Pair<Level, String>> getLogEntries()
    {
        return this.logEntries;
    }
}
