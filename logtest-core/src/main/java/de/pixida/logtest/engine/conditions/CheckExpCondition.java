/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine.conditions;

import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.engine.EmbeddedScript;
import de.pixida.logtest.engine.TimingInfo;

public class CheckExpCondition extends BaseCondition
{
    private static final Logger LOG = LoggerFactory.getLogger(CheckExpCondition.class);

    private EmbeddedScript script;

    public CheckExpCondition()
    {
        // Empty constructor needed by checkstyle
    }

    @Override
    public void init(final IEdgeDefinition edgeDefinition, final IParameters parameters, final ScriptEngine scriptingEngine)
    {
        this.script = new EmbeddedScript(edgeDefinition.getCheckExp());
        this.setIsActive(this.script.exists());
        if (this.isActive())
        {
            this.script.compile(scriptingEngine);
        }
    }

    @Override
    public boolean evaluate(final IEventDescription eventDescription, final TimingInfo timingInfo)
    {
        final boolean result = this.script.runAndGetBooleanResult();
        LOG.debug("Check expression evaluated to '{}'", result);
        return result;
    }

    @Override
    public boolean isApplicable(final IEventDescription eventDescription)
    {
        return eventDescription.isLogEntry();
    }
}
