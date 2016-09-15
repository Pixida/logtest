/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.processing;

import java.util.List;

import de.pixida.logtest.logreaders.ILogReader;

/** A job links 1 log reader with n automatons */
public class Job
{
    private ILogReader logReader;
    private List<LogSink> sinks;

    public Job()
    {
        // Empty constructor needed by checkstyle
    }

    public ILogReader getLogReader()
    {
        return this.logReader;
    }

    public void setLogReader(final ILogReader value)
    {
        this.logReader = value;
    }

    public List<LogSink> getSinks()
    {
        return this.sinks;
    }

    public void setSinks(final List<LogSink> value)
    {
        this.sinks = value;
    }
}
