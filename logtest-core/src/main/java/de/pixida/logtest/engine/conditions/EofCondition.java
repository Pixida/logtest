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
import de.pixida.logtest.engine.TimingInfo;

public class EofCondition extends BaseCondition
{
    private static final Logger LOG = LoggerFactory.getLogger(EofCondition.class);

    private boolean isOn;

    public EofCondition()
    {
        // Empty constructor needed by checkstyle
    }

    @Override
    public void init(final IEdgeDefinition edgeDefinition, final IParameters parameters, final ScriptEngine scriptingEngine)
    {
        this.setIsActive(edgeDefinition.getTriggerOnEof() != null);
        if (this.isActive())
        {
            this.isOn = edgeDefinition.getTriggerOnEof();
        }
    }

    @Override
    public boolean evaluate(final IEventDescription eventDescription, final TimingInfo timingInfo)
    {
        // This condition always triggers on EOF events, if set to active
        if (this.isOn)
        {
            LOG.trace("Condition EOF matches");
            return true;
        }
        return false;
    }

    @Override
    public boolean isApplicable(final IEventDescription eventDescription)
    {
        return eventDescription.isEof();
    }
}
