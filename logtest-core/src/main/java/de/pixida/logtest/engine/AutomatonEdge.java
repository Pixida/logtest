/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.script.ScriptEngine;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.engine.conditions.AlwaysTriggeringCondition;
import de.pixida.logtest.engine.conditions.CheckExpCondition;
import de.pixida.logtest.engine.conditions.EofCondition;
import de.pixida.logtest.engine.conditions.ICondition;
import de.pixida.logtest.engine.conditions.IEventDescription;
import de.pixida.logtest.engine.conditions.IScriptEnvironment;
import de.pixida.logtest.engine.conditions.RegExpCondition;
import de.pixida.logtest.engine.conditions.TimeIntervalCondition;

class AutomatonEdge
{
    private static final Logger LOG = LoggerFactory.getLogger(AutomatonEdge.class);

    enum RequiredConditions
    {
        ALL,
        ONE
    }

    private final IEdgeDefinition externalEdge;

    private AutomatonNode sourceNode;
    private AutomatonNode destinationNode;

    private EmbeddedScript onWalk;
    private String channel;

    private RequiredConditions requiredConditionsSetting;

    private final List<ICondition> conditions = new ArrayList<>();
    private final List<ICondition> activeConditions = new ArrayList<>();

    AutomatonEdge(final IEdgeDefinition edge)
    {
        Validate.notNull(edge);
        this.externalEdge = edge;
    }

    void setDestination(final AutomatonNode value)
    {
        Validate.notNull(value);
        this.destinationNode = value;
    }

    void setSource(final AutomatonNode value)
    {
        Validate.notNull(value);
        this.sourceNode = value;
    }

    AutomatonNode getSourceNode()
    {
        return this.sourceNode;
    }

    AutomatonNode getDestinationNode()
    {
        return this.destinationNode;
    }

    EmbeddedScript getOnWalk()
    {
        return this.onWalk;
    }

    void setOnWalk(final EmbeddedScript value)
    {
        this.onWalk = value;
    }

    RequiredConditions getRequiredConditionsSetting()
    {
        return this.requiredConditionsSetting;
    }

    void setRequiredConditionsSetting(final RequiredConditions value)
    {
        this.requiredConditionsSetting = value;
    }

    void setChannel(final String value)
    {
        this.channel = value;
    }

    void initConditions(final AutomatonParameters parameters, final ScriptEngine scriptingEngine)
    {
        Validate.notNull(parameters);
        Validate.notNull(scriptingEngine);

        // Create and initialize conditions
        this.conditions.add(new AlwaysTriggeringCondition());
        this.conditions.add(new EofCondition());
        this.conditions.add(new CheckExpCondition());
        this.conditions.add(new RegExpCondition());
        this.conditions.add(new TimeIntervalCondition(edgeDef -> edgeDef.getTimeIntervalSinceLastMicrotransition(),
            timingInfo -> timingInfo.getTimeOfLastMicrotransition()));
        this.conditions.add(new TimeIntervalCondition(edgeDef -> edgeDef.getTimeIntervalSinceLastTransition(),
            timingInfo -> timingInfo.getTimeOfLastTransition()));
        this.conditions.add(new TimeIntervalCondition(edgeDef -> edgeDef.getTimeIntervalSinceAutomatonStart(),
            timingInfo -> timingInfo.getStartTime()));
        this.conditions.add(new TimeIntervalCondition(edgeDef -> edgeDef.getTimeIntervalForEvent(), timingInfo -> 0L));
        try
        {
            this.conditions.stream().forEach(condition -> condition.init(this.externalEdge, parameters.getDeepCopy(), scriptingEngine));
        }
        catch (final InvalidAutomatonDefinitionException iade)
        {
            throw new InvalidAutomatonDefinitionException("Failed to initialize condition on edge '" + this + "': " + iade.getMessage(),
                iade.getCause());
        }

        // Create a list of conditions which are configured for this edge ("active" conditions which are considered when a log event occurs)
        this.conditions.stream().filter(condition -> condition.isActive()).forEach(condition -> this.activeConditions.add(condition));
    }

    boolean hasActiveConditions()
    {
        return this.activeConditions.size() > 0;
    }

    boolean edgeMatchesEvent(final IEventDescription eventDescription, final TimingInfo timingInfo)
    {
        LOG.trace("Checking if edge '{}' matches", this);
        boolean edgeMatches = false;

        // Check if channel matches
        final String eventChannel = eventDescription.getChannel();
        if (eventDescription.getSupportsChannels() && !StringUtils.equals(this.channel, eventChannel))
        {
            LOG.debug("Event channel '{}' does not match edge channel '{}'", eventChannel, this.channel);
        }
        else
        {
            // Fetch mode (ONE/OR or ALL/AND)
            RequiredConditions currentRequiredConditions = this.requiredConditionsSetting;
            if (currentRequiredConditions == null)
            {
                final RequiredConditions defaultValueIfNoneIsSetInEdgeConfiguration = RequiredConditions.ALL;
                currentRequiredConditions = defaultValueIfNoneIsSetInEdgeConfiguration;
            }

            // Check if this edge matches
            final long numApplicableConditions = this.activeConditions.stream()
                .filter(condition -> condition.isApplicable(eventDescription)).count();
            long numMatchingConditions = 0;
            if (numApplicableConditions > 0)
            {
                final Stream<ICondition> applicableConditions = this.activeConditions.stream()
                    .filter(condition -> condition.isApplicable(eventDescription));
                numMatchingConditions = applicableConditions.filter(condition -> condition.evaluate(eventDescription, timingInfo)).count();
                if (currentRequiredConditions == RequiredConditions.ALL)
                {
                    edgeMatches = numMatchingConditions == numApplicableConditions;
                }
                else if (currentRequiredConditions == RequiredConditions.ONE)
                {
                    edgeMatches = numMatchingConditions > 0;
                }
            }

            // Some debug output which has proven to be helpful...
            LOG.debug("Edge '{}' conditions: Total {}, Active {}, Applicable {}, Matching {}",
                this, this.conditions.size(), this.activeConditions.size(), numApplicableConditions, numMatchingConditions);
        }

        LOG.trace("Edge matches: {}", edgeMatches);
        return edgeMatches;
    }

    public void beforeOnWalk(final IScriptEnvironment scriptEnvironment)
    {
        this.conditions.stream().forEach(condition -> condition.beforeOnWalk(scriptEnvironment));
    }

    public void afterOnWalk(final IScriptEnvironment scriptEnvironment)
    {
        this.conditions.stream().forEach(condition -> condition.afterOnWalk(scriptEnvironment));
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return this.externalEdge.toString();
    }
}
