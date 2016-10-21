/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import java.util.HashMap;
import java.util.Map;

import de.pixida.logtest.automatondefinitions.PregCallbackReplacer;
import de.pixida.logtest.engine.conditions.IParameters;

public class AutomatonParameters implements IParameters
{
    public static final String PARAMETER_PREG = "\\$\\{(.*?)\\}";

    private final Map<String, String> parameters;

    AutomatonParameters(final Map<String, String> map)
    {
        this.parameters = new HashMap<>(map); // Create deep copy
    }

    String get(final String key)
    {
        return this.parameters.get(key);
    }

    @Override
    public String insertAllParameters(final String value)
    {
        if (value == null)
        {
            return null;
        }
        else
        {
            return new PregCallbackReplacer(PARAMETER_PREG).replaceMatches(value, match -> {
                final String paramName = match.group(1);
                final String v = this.parameters.get(paramName);
                if (v == null)
                {
                    throw new InvalidAutomatonDefinitionException("Referenced parameter '" + paramName + "' is not defined");
                }
                else
                {
                    return v;
                }
            });
        }
    }

    AutomatonParameters getDeepCopy()
    {
        return new AutomatonParameters(this.parameters);
    }
}
