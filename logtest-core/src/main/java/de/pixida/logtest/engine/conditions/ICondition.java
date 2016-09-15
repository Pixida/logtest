/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine.conditions;

import javax.script.ScriptEngine;

import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.engine.TimingInfo;

public interface ICondition
{
    /**
     * Initialize condition, called once for every condition on every edge
     *
     * @param edgeDefinition
     *            Raw definition of the edge
     * @param parameters
     *            Parameters for the automaton evaluation
     * @param scriptingEngine
     *            Scripting engine to be used
     */
    void init(final IEdgeDefinition edgeDefinition, IParameters parameters, ScriptEngine scriptingEngine);

    /**
     * Returns whether the initialized condition is active on the edge, i.e. it must be evaluated
     *
     * @return True if this initialized condition is active on the edge
     */
    boolean isActive();

    /**
     * Returns whether the initialized and active condition is applicable for an event
     *
     * @param eventDescription
     *            Event
     * @return True if the condition is applicable for the given event
     */
    boolean isApplicable(IEventDescription eventDescription);

    /**
     * Check if the initialized and active and applicable condition is triggered by an event.
     *
     * @param eventDescription
     *            Event
     * @param timingInfo
     *            Current timing information
     * @return True if the condition matches
     */
    boolean evaluate(IEventDescription eventDescription, TimingInfo timingInfo);

    /**
     * Initialize script environment before 'onWalk' handler is called. Note that the condition must neither be active, applicable nor
     * matching when the method is called!
     *
     * @param scriptEnvironment
     *            Environment variables for the script
     */
    void beforeOnWalk(IScriptEnvironment scriptEnvironment);

    /**
     * Reset script environment after 'onWalk' handler is called. Note that the condition must neither be active, applicable nor matching
     * when the method is called!
     *
     * @param scriptEnvironment
     *            Environment variables for the script
     */
    void afterOnWalk(IScriptEnvironment scriptEnvironment);
}
