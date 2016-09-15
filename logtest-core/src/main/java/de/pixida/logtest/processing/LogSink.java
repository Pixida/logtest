/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.processing;

import java.util.Map;
import java.util.stream.Collectors;

import de.pixida.logtest.automatondefinitions.IAutomatonDefinition;

/** Automaton and its configuration via parameters */
public class LogSink
{
    private IAutomatonDefinition automaton;
    private Map<String, String> parameters;

    public LogSink()
    {
        // Empty constructor needed by checkstyle
    }

    public IAutomatonDefinition getAutomaton()
    {
        return this.automaton;
    }

    public void setAutomaton(final IAutomatonDefinition value)
    {
        this.automaton = value;
    }

    public Map<String, String> getParameters()
    {
        return this.parameters;
    }

    public void setParameters(final Map<String, String> value)
    {
        this.parameters = value;
    }

    public String getDisplayName()
    {
        String paramString;
        if (this.parameters.isEmpty())
        {
            paramString = "";
        }
        else
        {
            paramString = String.format("(%s)",
                this.parameters.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(",")));
        }
        return this.automaton.getDisplayName() + paramString;
    }
}
