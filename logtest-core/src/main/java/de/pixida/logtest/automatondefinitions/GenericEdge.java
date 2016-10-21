/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import org.apache.commons.lang3.StringUtils;

public class GenericEdge implements IEdgeDefinition
{
    private final String id;
    private String name;
    private String description;

    private final INodeDefinition source;
    private final INodeDefinition destination;

    private String regExp;
    private String checkExp;
    private Boolean triggerAlways;
    private String onWalk;
    private Boolean triggerOnEof;
    private RequiredConditions requiredCondition;
    private ITimeInterval timeIntervalSinceLastMicrotransition;
    private ITimeInterval timeIntervalSinceLastTransition;
    private ITimeInterval timeIntervalSinceAutomatonStart;
    private ITimeInterval timeIntervalForEvent;
    private String channel;

    private String nameForLogging;

    public GenericEdge(final String aId, final INodeDefinition aSource, final INodeDefinition aDestination)
    {
        this.id = aId;
        this.source = aSource;
        this.destination = aDestination;
        this.createNameForLogging();
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public INodeDefinition getSource()
    {
        return this.source;
    }

    @Override
    public INodeDefinition getDestination()
    {
        return this.destination;
    }

    @Override
    public String getRegExp()
    {
        return this.regExp;
    }

    public void setRegExp(final String value)
    {
        this.regExp = value;
    }

    @Override
    public String getCheckExp()
    {
        return this.checkExp;
    }

    public void setCheckExp(final String value)
    {
        this.checkExp = value;
    }

    @Override
    public Boolean getTriggerAlways()
    {
        return this.triggerAlways;
    }

    public void setTriggerAlways(final Boolean value)
    {
        this.triggerAlways = value;
    }

    @Override
    public String getOnWalk()
    {
        return this.onWalk;
    }

    public void setOnWalk(final String value)
    {
        this.onWalk = value;
    }

    @Override
    public Boolean getTriggerOnEof()
    {
        return this.triggerOnEof;
    }

    public void setTriggerOnEof(final Boolean value)
    {
        this.triggerOnEof = value;
    }

    @Override
    public RequiredConditions getRequiredConditions()
    {
        return this.requiredCondition;
    }

    public void setRequiredConditions(final RequiredConditions value)
    {
        this.requiredCondition = value;
    }

    @Override
    public ITimeInterval getTimeIntervalSinceLastMicrotransition()
    {
        return this.timeIntervalSinceLastMicrotransition;
    }

    public void setTimeIntervalSinceLastMicrotransition(final ITimeInterval value)
    {
        this.timeIntervalSinceLastMicrotransition = value;
    }

    @Override
    public ITimeInterval getTimeIntervalSinceLastTransition()
    {
        return this.timeIntervalSinceLastTransition;
    }

    public void setTimeIntervalSinceLastTransition(final ITimeInterval value)
    {
        this.timeIntervalSinceLastTransition = value;
    }

    @Override
    public ITimeInterval getTimeIntervalSinceAutomatonStart()
    {
        return this.timeIntervalSinceAutomatonStart;
    }

    public void setTimeIntervalSinceAutomatonStart(final ITimeInterval value)
    {
        this.timeIntervalSinceAutomatonStart = value;
    }

    @Override
    public ITimeInterval getTimeIntervalForEvent()
    {
        return this.timeIntervalForEvent;
    }

    public void setTimeIntervalForEvent(final ITimeInterval value)
    {
        this.timeIntervalForEvent = value;
    }

    @Override
    public String getChannel()
    {
        return this.channel;
    }

    public void setChannel(final String value)
    {
        this.channel = value;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    public void setName(final String value)
    {
        this.name = value;
        this.createNameForLogging();
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(final String value)
    {
        this.description = value;
    }

    private void createNameForLogging()
    {
        if (StringUtils.isNotBlank(this.name))
        {
            this.nameForLogging = this.replaceWhitespacesAgainstSpace(this.name);
        }
        else if (StringUtils.isNotBlank(this.id))
        {
            this.nameForLogging = this.replaceWhitespacesAgainstSpace(this.id);
        }
        else
        {
            this.nameForLogging = "";
        }
    }

    private String replaceWhitespacesAgainstSpace(final String value)
    {
        return value.replaceAll("\\s", " ");
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return this.nameForLogging;
    }
}
