/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONObject;

import de.pixida.logtest.automatondefinitions.IEdgeDefinition.RequiredConditions;
import de.pixida.logtest.logreaders.ILogEntry;

public abstract class AutomatonDefinitionToJsonConverter
{
    public static JSONObject convert(final IAutomatonDefinition automaton)
    {
        final JSONObject root = new JSONObject();
        putValueIfNotNull(root, JsonKey.ROOT_ONLOAD, automaton.getOnLoad());
        putValueIfNotNull(root, JsonKey.ROOT_COMMENT, automaton.getComment());
        putValueIfNotNull(root, JsonKey.ROOT_SCRIPT_LANGUAGE, automaton.getScriptLanguage());
        root.put(JsonKey.ROOT_NODES.getKey(), convertNodes(automaton.getNodes(), automaton.getEdges()));
        return root;
    }

    private static JSONArray convertNodes(final List<? extends INodeDefinition> definedNodes,
        final List<? extends IEdgeDefinition> definedEdges)
    {
        final JSONArray nodes = new JSONArray();
        for (final INodeDefinition definedNode : definedNodes)
        {
            final JSONObject node = convertNode(definedNode);

            final JSONArray outgoingEdges = new JSONArray();
            for (final IEdgeDefinition definedOutgoingEdge : definedEdges.stream().filter(edge -> edge.getSource() == definedNode)
                .collect(Collectors.toList()))
            {
                outgoingEdges.put(convertEdge(definedOutgoingEdge));
            }
            if (outgoingEdges.length() > 0)
            {
                node.put(JsonKey.NODE_OUTGOING_EDGES.getKey(), outgoingEdges);
            }

            nodes.put(node);
        }
        return nodes;
    }

    private static JSONObject convertNode(final INodeDefinition definedNode)
    {
        final JSONObject node = new JSONObject();
        node.put(JsonKey.NODE_ID.getKey(), definedNode.toString());
        putTrueValueIfTrue(node, JsonKey.NODE_INITIAL, definedNode.getFlags().contains(INodeDefinition.Flag.IS_INITIAL));
        putTrueValueIfTrue(node, JsonKey.NODE_SUCCESS, definedNode.getFlags().contains(INodeDefinition.Flag.IS_SUCCESS));
        putTrueValueIfTrue(node, JsonKey.NODE_FAILURE, definedNode.getFlags().contains(INodeDefinition.Flag.IS_FAILURE));
        putValueIfNotNull(node, JsonKey.NODE_ON_ENTER, definedNode.getOnEnter());
        putValueIfNotNull(node, JsonKey.NODE_ON_LEAVE, definedNode.getOnLeave());
        putValueIfNotNull(node, JsonKey.NODE_SUCCESS_CHECK_EXP, definedNode.getSuccessCheckExp());
        putValueIfNotNull(node, JsonKey.NODE_COMMENT, definedNode.getComment());
        putTrueValueIfTrue(node, JsonKey.NODE_WAIT, definedNode.getWait());
        return node;
    }

    private static JSONObject convertEdge(final IEdgeDefinition definedOutgoingEdge)
    {
        final JSONObject edge = new JSONObject();
        edge.put(JsonKey.EDGE_ID.getKey(), definedOutgoingEdge.toString());
        edge.put(JsonKey.EDGE_DESTINATION.getKey(), definedOutgoingEdge.getDestination().toString());
        putValueIfNotNull(edge, JsonKey.EDGE_ON_WALK, definedOutgoingEdge.getOnWalk());
        putValueIfNotNull(edge, JsonKey.EDGE_COMMENT, definedOutgoingEdge.getComment());
        putValueIfNotNull(edge, JsonKey.EDGE_REG_EXP, definedOutgoingEdge.getRegExp());
        putValueIfNotNull(edge, JsonKey.EDGE_CHECK_EXP, definedOutgoingEdge.getCheckExp());
        putTrueValueIfNotNullAndTrue(edge, JsonKey.EDGE_TRIGGER_ALWAYS, definedOutgoingEdge.getTriggerAlways());
        putTrueValueIfNotNullAndTrue(edge, JsonKey.EDGE_TRIGGER_ON_EOF, definedOutgoingEdge.getTriggerOnEof());
        if (definedOutgoingEdge.getRequiredConditions() != null)
        {
            String requiredConditionsValue = null;
            if (definedOutgoingEdge.getRequiredConditions() == RequiredConditions.ONE)
            {
                requiredConditionsValue = JsonKey.EDGE_REQUIRED_CONDITIONS_ONE.getKey();
            }
            else if (definedOutgoingEdge.getRequiredConditions() == RequiredConditions.ALL)
            {
                requiredConditionsValue = JsonKey.EDGE_REQUIRED_CONDITIONS_ALL.getKey();
            }
            else
            {
                throw new RuntimeException("Internal error: Cannot save value for required conditions - value missing for "
                    + definedOutgoingEdge.getRequiredConditions());
            }
            edge.put(JsonKey.EDGE_REQUIRED_CONDITIONS.getKey(), requiredConditionsValue);
        }
        putTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_LAST_MICROTRANSITION,
            definedOutgoingEdge.getTimeIntervalSinceLastMicrotransition());
        putTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_LAST_TRANSITION, definedOutgoingEdge.getTimeIntervalSinceLastTransition());
        putTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_AUTOMATON_START, definedOutgoingEdge.getTimeIntervalSinceAutomatonStart());
        putTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_FOR_EVENT, definedOutgoingEdge.getTimeIntervalForEvent());
        if (definedOutgoingEdge.getChannel() != ILogEntry.DEFAULT_CHANNEL)
        {
            edge.put(JsonKey.EDGE_CHANNEL.getKey(), definedOutgoingEdge.getChannel());
        }
        return edge;
    }

    private static void putTimeInterval(final JSONObject edge, final JsonKey timeIntervalKey, final ITimeInterval definedTimeInterval)
    {
        if (definedTimeInterval != null && (definedTimeInterval.getMin() != null || definedTimeInterval.getMax() != null))
        {
            final JSONObject interval = new JSONObject();
            putDuration(interval, JsonKey.TIME_INTERVAL_MIN, definedTimeInterval.getMin());
            putDuration(interval, JsonKey.TIME_INTERVAL_MAX, definedTimeInterval.getMax());
            edge.put(timeIntervalKey.getKey(), interval);
        }
    }

    private static void putDuration(final JSONObject interval, final JsonKey durationKey, final IDuration definedDuration)
    {
        if (definedDuration != null)
        {
            final JSONObject duration = new JSONObject();
            duration.put(JsonKey.TIME_INTERVAL_DURATION_IS_INCLUSIVE.getKey(), definedDuration.isInclusive());
            duration.put(JsonKey.TIME_INTERVAL_DURATION_VALUE.getKey(), definedDuration.getValue());
            final String durationUnit = JsonTimeUnit.convertTimeUnitToString(definedDuration.getUnit());
            if (durationUnit == null)
            {
                throw new RuntimeException("Internal error: Cannot write unit of timespan duration as it is not defined");
            }
            duration.put(JsonKey.TIME_INTERVAL_DURATION_UNIT.getKey(), durationUnit);

            interval.put(durationKey.getKey(), duration);
        }
    }

    private static <T> void putValueIfNotNull(final JSONObject jsonObject, final JsonKey jsonKey, final T value)
    {
        Validate.notNull(jsonObject);
        Validate.notNull(jsonKey);
        if (value != null)
        {
            jsonObject.put(jsonKey.getKey(), value);
        }
    }

    private static <T> void putTrueValueIfTrue(final JSONObject jsonObject, final JsonKey jsonKey, final boolean value)
    {
        Validate.notNull(jsonObject);
        Validate.notNull(jsonKey);
        if (value)
        {
            jsonObject.put(jsonKey.getKey(), value);
        }
    }

    private static <T> void putTrueValueIfNotNullAndTrue(final JSONObject jsonObject, final JsonKey jsonKey, final Boolean value)
    {
        Validate.notNull(jsonObject);
        Validate.notNull(jsonKey);
        if (BooleanUtils.isTrue(value))
        {
            jsonObject.put(jsonKey.getKey(), value);
        }
    }
}
