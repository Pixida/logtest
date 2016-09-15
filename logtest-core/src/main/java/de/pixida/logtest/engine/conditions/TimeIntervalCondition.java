/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine.conditions;

import java.util.function.Function;

import javax.script.ScriptEngine;

import org.apache.commons.lang3.Validate;

import de.pixida.logtest.automatondefinitions.IDuration;
import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.automatondefinitions.ITimeInterval;
import de.pixida.logtest.engine.InvalidAutomatonDefinitionException;
import de.pixida.logtest.engine.TimingInfo;

public class TimeIntervalCondition extends BaseCondition
{
    private final Function<IEdgeDefinition, ITimeInterval> timeIntervalGetter;
    private final Function<TimingInfo, Long> timeGetter;

    private ITimeInterval timeInterval;
    private long minValue;
    private long maxValue;

    public TimeIntervalCondition(final Function<IEdgeDefinition, ITimeInterval> aTimeIntervalGetter, final Function<TimingInfo, Long> aTimeGetter)
    {
        Validate.notNull(aTimeIntervalGetter);
        Validate.notNull(aTimeGetter);
        this.timeIntervalGetter = aTimeIntervalGetter;
        this.timeGetter = aTimeGetter;
    }

    @Override
    public void init(final IEdgeDefinition edgeDefinition, final IParameters parameters, final ScriptEngine scriptingEngine)
    {
        this.timeInterval = this.timeIntervalGetter.apply(edgeDefinition);
        this.setIsActive(this.timeInterval != null);
        if (this.isActive())
        {
            this.parseValues(parameters);
            this.checkTimeInterval();
        }
    }

    private void parseValues(final IParameters parameters)
    {
        if (this.timeInterval.getMin() != null)
        {
            this.minValue = this.parseTimeValue(this.timeInterval.getMin(), parameters);
        }
        if (this.timeInterval.getMax() != null)
        {
            this.maxValue = this.parseTimeValue(this.timeInterval.getMax(), parameters);
        }
    }

    private long parseTimeValue(final IDuration duration, final IParameters parameters)
    {
        String value = duration.getValue();
        if (value == null)
        {
            throw new InvalidAutomatonDefinitionException("Duration value is not set!");
        }
        try
        {
            value = parameters.insertAllParameters(value);
            return Long.parseLong(value);
        }
        catch (final NumberFormatException nfe)
        {
            throw new InvalidAutomatonDefinitionException("Invalid duration value in condition found: " + value);
        }
    }

    private void checkTimeInterval()
    {
        if (this.timeInterval.getMin() == null && this.timeInterval.getMax() == null)
        {
            throw new InvalidAutomatonDefinitionException("Neither min nor max durations are set for time interval");
        }
        if (this.timeInterval.getMin() != null)
        {
            this.checkDuration(this.timeInterval.getMin());
        }
        if (this.timeInterval.getMax() != null)
        {
            this.checkDuration(this.timeInterval.getMax());
        }

        if (this.timeInterval.getMin() != null && this.timeInterval.getMax() != null)
        {
            if (this.minValue > this.maxValue)
            {
                throw new InvalidAutomatonDefinitionException("Min time > max time in time interval");
            }
            final boolean minIsInclusive = this.timeInterval.getMin().isInclusive();
            final boolean maxIsInclusive = this.timeInterval.getMax().isInclusive();
            if (this.minValue == this.maxValue)
            {
                final boolean someBoundIsNotInclusive = !minIsInclusive || !maxIsInclusive;
                if (someBoundIsNotInclusive)
                {
                    throw new InvalidAutomatonDefinitionException(
                        "Min time = max time but one boundary or both are open; condition would never match");
                }
            }
            if (this.minValue + 1 == this.maxValue)
            {
                final boolean bothBoundsAreExclusive = !minIsInclusive && !maxIsInclusive;
                if (bothBoundsAreExclusive)
                {
                    throw new InvalidAutomatonDefinitionException(
                        "Min time = max time - 1 but both boundaries are open; condition would never match");
                }
            }
        }
    }

    private void checkDuration(final IDuration duration)
    {
        if (duration.getUnit() == null)
        {
            throw new InvalidAutomatonDefinitionException("Invalid unit value: " + duration.getUnit());
        }
    }

    @Override
    public boolean evaluate(final IEventDescription eventDescription, final TimingInfo timingInfo)
    {
        final long timeMs = eventDescription.getLogEntryTime() - this.timeGetter.apply(timingInfo);
        final boolean minMatches = this.checkElapsedTimeMsMatchesDuration(this.timeInterval.getMin(), true, timeMs);
        final boolean maxMatches = this.checkElapsedTimeMsMatchesDuration(this.timeInterval.getMax(), false, timeMs);
        return minMatches && maxMatches;
    }

    private boolean checkElapsedTimeMsMatchesDuration(final IDuration checkDuration, final boolean isMin, final long timeMs)
    {
        if (checkDuration == null)
        {
            // Min or max not defined => Interval has no limits => Matching.
            return true;
        }
        final long ms = isMin ? this.minValue : this.maxValue;
        if (isMin)
        {
            if (checkDuration.isInclusive())
            {
                return timeMs >= ms;
            }
            else
            {
                return timeMs > ms;
            }
        }
        else
        {
            if (checkDuration.isInclusive())
            {
                return timeMs <= ms;
            }
            else
            {
                return timeMs < ms;
            }
        }
    }

    @Override
    public boolean isApplicable(final IEventDescription eventDescription)
    {
        return eventDescription.isLogEntry();
    }
}
