/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.processing;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import de.pixida.logtest.logreaders.ILogEntry;
import de.pixida.logtest.logreaders.ILogReader;

public class TestLogReader implements ILogReader
{
    private final List<ILogEntry> entries = new ArrayList<>();
    private int entry = 0;

    TestLogReader()
    {
        // Empty constructor needed by checkstyle
    }

    void addEntry(final ILogEntry value)
    {
        this.entries.add(value);
    }

    @Override
    public void overwriteCurrentSettingsWithSettingsInConfigurationFile(final JSONObject configurationFile)
    {
    }

    @Override
    public void open()
    {
        this.entry = 0;
    }

    @Override
    public ILogEntry getNextEntry()
    {
        return this.entries.get(this.entry++);
    }

    @Override
    public void close()
    {
    }

    @Override
    public String getDisplayName()
    {
        return "Test log reader";
    }
}
