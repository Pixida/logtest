/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.engine.InvalidAutomatonDefinitionException;
import de.pixida.logtest.engine.TimingInfo;

public class RegExpCondition extends BaseCondition
{
    private static final Logger LOG = LoggerFactory.getLogger(RegExpCondition.class);

    private String regExp;
    private Pattern pattern;
    private List<String> matchingGroups;

    public RegExpCondition()
    {
        // Empty constructor needed by checkstyle
    }

    @Override
    public void init(final IEdgeDefinition edgeDefinition, final IParameters parameters, final ScriptEngine scriptingEngine)
    {
        this.setIsActive(edgeDefinition.getRegExp() != null);
        if (this.isActive())
        {
            this.regExp = parameters.insertAllParameters(edgeDefinition.getRegExp());
            try
            {
                this.pattern = Pattern.compile(this.regExp);
            }
            catch (final PatternSyntaxException pse)
            {
                throw new InvalidAutomatonDefinitionException("Invalid regular expression pattern", pse);
            }
        }
    }

    @Override
    public boolean evaluate(final IEventDescription eventDescription, final TimingInfo timingInfo,
        final IScriptEnvironment scriptEnvironment)
    {
        final Matcher matcher = this.pattern.matcher(eventDescription.getLogEntryPayload());
        if (matcher.find())
        {
            if (this.matchingGroups == null)
            {
                this.matchingGroups = new ArrayList<>(matcher.groupCount() + 1); // +1 as group 0 not included into this count
            }
            else
            {
                this.matchingGroups.clear();
            }
            for (int i = 0; i <= matcher.groupCount(); i++)
            {
                this.matchingGroups.add(matcher.group(i));
            }
            LOG.debug("Pattern '{}' matched with groups '{}'", this.regExp, this.matchingGroups);

            // Push matching groups into script environment so that they can be used in conditions evaluated later
            scriptEnvironment.setRegExpConditionMatchingGroups(this.matchingGroups);

            return true;
        }
        else
        {
            if (this.matchingGroups != null)
            {
                this.matchingGroups.clear();
            }
            LOG.debug("Pattern '{}' did not match", this.regExp);
            return false;
        }
    }

    @Override
    public boolean isApplicable(final IEventDescription eventDescription)
    {
        return eventDescription.isLogEntry();
    }

    @Override
    public void beforeOnWalk(final IScriptEnvironment scriptEnvironment)
    {
        final boolean weHadAMatch = this.matchingGroups != null && this.matchingGroups.size() > 0;
        final List<String> matchings = weHadAMatch ? this.matchingGroups : null;
        LOG.debug("Setting current matching group for script environment before on walk: {}", matchings);
        scriptEnvironment.setRegExpConditionMatchingGroups(matchings);
    }

    @Override
    public void afterOnWalk(final IScriptEnvironment scriptEnvironment)
    {
        scriptEnvironment.setRegExpConditionMatchingGroups(null);
    }
}
