/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.logreaders.ILogEntry;

public class JsonAutomatonDefinition implements IAutomatonDefinition
{
    public static final Charset EXPECTED_CHARSET = StandardCharsets.UTF_8;

    private static final Logger LOG = LoggerFactory.getLogger(JsonAutomatonDefinition.class);

    private final String name;
    private final File jsonFile;
    private final String jsonData;

    private boolean automatonLoaded = false;
    private final List<GenericNode> nodes = new ArrayList<>();
    private final List<GenericEdge> edges = new ArrayList<>();
    private final Map<String, GenericNode> mapIdNode = new HashMap<>();
    private String onLoad;
    private String comment;
    private String scriptLanguage;

    public JsonAutomatonDefinition(final File aJsonFile)
    {
        Validate.notNull(aJsonFile);
        this.jsonFile = aJsonFile;
        this.name = this.jsonFile.getName();
        this.jsonData = null;
        LOG.debug("Constructed loader from JSON file: {}", this.jsonFile.getAbsolutePath());
    }

    public JsonAutomatonDefinition(final String aName, final String data)
    {
        Validate.notNull(aName);
        Validate.notNull(data);
        this.jsonFile = null;
        this.name = aName;
        this.jsonData = data;
        LOG.debug("Constructed loader of automaton '{}' from data '{}' (length: '{}')", this.name, this.jsonData, this.jsonData.length());
    }

    @Override
    public List<INodeDefinition> getNodes()
    {
        return new ArrayList<>(this.nodes);
    }

    @Override
    public List<IEdgeDefinition> getEdges()
    {
        return new ArrayList<>(this.edges);
    }

    @Override
    public String getOnLoad()
    {
        return this.onLoad;
    }

    @Override
    public String getDisplayName()
    {
        return this.jsonFile.getName();
    }

    @Override
    public String getComment()
    {
        return this.comment;
    }

    @Override
    public String getScriptLanguage()
    {
        return this.scriptLanguage;
    }

    @Override
    public void load()
    {
        if (!this.automatonLoaded)
        {
            try
            {
                final String data = this.jsonData == null ? FileUtils.readFileToString(this.jsonFile, EXPECTED_CHARSET) : this.jsonData;
                final JSONObject root = new JSONObject(data);
                this.onLoad = root.has(JsonKey.ROOT_ONLOAD.getKey()) ? root.getString(JsonKey.ROOT_ONLOAD.getKey()) : null;
                this.comment = root.has(JsonKey.ROOT_COMMENT.getKey()) ? root.getString(JsonKey.ROOT_COMMENT.getKey()) : null;
                this.scriptLanguage = root.has(JsonKey.ROOT_SCRIPT_LANGUAGE.getKey()) ? root
                    .getString(JsonKey.ROOT_SCRIPT_LANGUAGE.getKey()) : null;
                this.extractNodesAndEdges(root);
                this.automatonLoaded = true;
            }
            catch (final IOException ioe)
            {
                throw new AutomatonLoadingException("Failed to read json file", ioe);
            }
            catch (final JSONException jsonEx)
            {
                throw new AutomatonLoadingException("Json data malformed (invalid syntax, missing attributes or wrong attribute types)",
                    jsonEx);
            }
        }
    }

    private void extractNodesAndEdges(final JSONObject root)
    {
        this.nodes.clear();
        this.edges.clear();
        this.mapIdNode.clear();
        final JSONArray jsonNodes = root.getJSONArray(JsonKey.ROOT_NODES.getKey());
        this.extractNodes(jsonNodes);
        this.extractEdges(jsonNodes);
    }

    private void extractNodes(final JSONArray jsonNodes)
    {
        for (int i = 0; i < jsonNodes.length(); i++)
        {
            final JSONObject node = jsonNodes.getJSONObject(i);
            final Set<INodeDefinition.Flag> flags = new HashSet<>();
            if (node.has(JsonKey.NODE_INITIAL.getKey()) && node.getBoolean(JsonKey.NODE_INITIAL.getKey()))
            {
                flags.add(INodeDefinition.Flag.IS_INITIAL);
            }
            if (node.has(JsonKey.NODE_FAILURE.getKey()) && node.getBoolean(JsonKey.NODE_FAILURE.getKey()))
            {
                flags.add(INodeDefinition.Flag.IS_FAILURE);
            }
            if (node.has(JsonKey.NODE_SUCCESS.getKey()) && node.getBoolean(JsonKey.NODE_SUCCESS.getKey()))
            {
                flags.add(INodeDefinition.Flag.IS_SUCCESS);
            }
            final GenericNode newNode = new GenericNode(node.getString("id"), flags);
            this.nodes.add(newNode);
            if (node.has(JsonKey.NODE_ON_ENTER.getKey()))
            {
                newNode.setOnEnter(node.getString(JsonKey.NODE_ON_ENTER.getKey()));
            }
            if (node.has(JsonKey.NODE_ON_LEAVE.getKey()))
            {
                newNode.setOnLeave(node.getString(JsonKey.NODE_ON_LEAVE.getKey()));
            }
            if (node.has(JsonKey.NODE_SUCCESS_CHECK_EXP.getKey()))
            {
                newNode.setSuccessCheckExp(node.getString(JsonKey.NODE_SUCCESS_CHECK_EXP.getKey()));
            }
            if (node.has(JsonKey.NODE_COMMENT.getKey()))
            {
                newNode.setComment(node.getString(JsonKey.NODE_COMMENT.getKey()));
            }
            newNode.setWait(node.has(JsonKey.NODE_WAIT.getKey()) ? node.getBoolean(JsonKey.NODE_WAIT.getKey()) : false);
            this.mapIdNode.put(node.getString(JsonKey.NODE_ID.getKey()), newNode);
        }
    }

    private void extractEdges(final JSONArray jsonNodes)
    {
        for (int i = 0; i < jsonNodes.length(); i++)
        {
            final JSONObject node = jsonNodes.getJSONObject(i);
            if (node.has(JsonKey.NODE_OUTGOING_EDGES.getKey()))
            {
                final JSONArray outgoingEdges = node.getJSONArray(JsonKey.NODE_OUTGOING_EDGES.getKey());
                for (int j = 0; j < outgoingEdges.length(); j++)
                {
                    final JSONObject edge = outgoingEdges.getJSONObject(j);
                    final GenericNode source = this.mapIdNode.get(node.getString(JsonKey.NODE_ID.getKey()));
                    final String destinationNodeId = edge.getString(JsonKey.EDGE_DESTINATION.getKey());
                    final GenericNode destination = this.mapIdNode.get(destinationNodeId);
                    assert source != null;
                    if (destination == null)
                    {
                        throw new AutomatonLoadingException("Destination node not found: " + destinationNodeId);
                    }
                    final GenericEdge newEdge = new GenericEdge(edge.getString(JsonKey.EDGE_ID.getKey()), source, destination);
                    if (edge.has(JsonKey.EDGE_ON_WALK.getKey()))
                    {
                        newEdge.setOnWalk(edge.getString(JsonKey.EDGE_ON_WALK.getKey()));
                    }
                    if (edge.has(JsonKey.EDGE_COMMENT.getKey()))
                    {
                        newEdge.setComment(edge.getString(JsonKey.EDGE_COMMENT.getKey()));
                    }
                    this.retrieveEdgeConditions(edge, newEdge);
                    this.edges.add(newEdge);
                }
            }
        }
    }

    private void retrieveEdgeConditions(final JSONObject edge, final GenericEdge newEdge)
    {
        if (edge.has(JsonKey.EDGE_REG_EXP.getKey()))
        {
            newEdge.setRegExp(edge.getString(JsonKey.EDGE_REG_EXP.getKey()));
        }
        if (edge.has(JsonKey.EDGE_CHECK_EXP.getKey()))
        {
            newEdge.setCheckExp(edge.getString(JsonKey.EDGE_CHECK_EXP.getKey()));
        }
        if (edge.has(JsonKey.EDGE_TRIGGER_ALWAYS.getKey()))
        {
            newEdge.setTriggerAlways(edge.getBoolean(JsonKey.EDGE_TRIGGER_ALWAYS.getKey()));
        }
        if (edge.has(JsonKey.EDGE_TRIGGER_ON_EOF.getKey()))
        {
            newEdge.setTriggerOnEof(edge.getBoolean(JsonKey.EDGE_TRIGGER_ON_EOF.getKey()));
        }
        if (edge.has(JsonKey.EDGE_REQUIRED_CONDITIONS.getKey()))
        {
            this.parseRequiredConditions(edge, newEdge);
        }
        newEdge
            .setTimeIntervalSinceLastMicrotransition(this.parseTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_LAST_MICROTRANSITION));
        newEdge.setTimeIntervalSinceLastTransition(this.parseTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_LAST_TRANSITION));
        newEdge.setTimeIntervalSinceAutomatonStart(this.parseTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_AUTOMATON_START));
        newEdge.setTimeIntervalForEvent(this.parseTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_FOR_EVENT));
        if (!edge.has(JsonKey.EDGE_CHANNEL.getKey()))
        {
            newEdge.setChannel(ILogEntry.DEFAULT_CHANNEL);
        }
        else
        {
            newEdge.setChannel(edge.getString(JsonKey.EDGE_CHANNEL.getKey()));
        }
    }

    private void parseRequiredConditions(final JSONObject edge, final GenericEdge newEdge)
    {
        final String val = edge.getString(JsonKey.EDGE_REQUIRED_CONDITIONS.getKey());
        if (val.equals(JsonKey.EDGE_REQUIRED_CONDITIONS_ONE.getKey()))
        {
            newEdge.setRequiredConditions(IEdgeDefinition.RequiredConditions.ONE);
        }
        else if (val.equals(JsonKey.EDGE_REQUIRED_CONDITIONS_ALL.getKey()))
        {
            newEdge.setRequiredConditions(IEdgeDefinition.RequiredConditions.ALL);
        }
        else
        {
            throw new AutomatonLoadingException("Invalid value for required conditions: " + val);
        }
    }

    private ITimeInterval parseTimeInterval(final JSONObject edge, final JsonKey key)
    {
        if (edge.has(key.getKey()))
        {
            final JSONObject timeIntervalObject = edge.optJSONObject(key.getKey());
            if (timeIntervalObject != null)
            {
                final GenericTimeInterval timeInterval = new GenericTimeInterval();
                timeInterval.setMin(this.parseDuration(timeIntervalObject, JsonKey.TIME_INTERVAL_MIN));
                timeInterval.setMax(this.parseDuration(timeIntervalObject, JsonKey.TIME_INTERVAL_MAX));
                if (timeInterval.getMin() == null && timeInterval.getMax() == null)
                {
                    return null;
                }
                else
                {
                    return timeInterval;
                }
            }

            // Dependency to automaton to JSON converter: Reading the time interval as a string only plays a role if the user typed the
            // JSON automaton definition by hand (unless the JSON converter supports this). It's not clear if this feature shall be
            // supported in future.
            // Above, we fetch "opt" JSONObject; when removing the following code, we can strictly assume it "is" a JSON object.
            final String timeIntervalString = edge.optString(key.getKey());
            if (timeIntervalString != null)
            {
                return StringToGenericTimeIntervalConverter.convertStringToTimestamp(timeIntervalString);
            }
        }

        return null;
    }

    private GenericDuration parseDuration(final JSONObject timeIntervalObject, final JsonKey key)
    {
        final JSONObject durationObject = timeIntervalObject.optJSONObject(key.getKey());
        if (durationObject == null)
        {
            return null;
        }
        else
        {
            final GenericDuration d = new GenericDuration();
            d.setInclusive(durationObject.getBoolean(JsonKey.TIME_INTERVAL_DURATION_IS_INCLUSIVE.getKey()));
            d.setValue(durationObject.getString(JsonKey.TIME_INTERVAL_DURATION_VALUE.getKey()));
            d.setUnit(
                StringToGenericTimeIntervalConverter.parseUnit(durationObject.getString(JsonKey.TIME_INTERVAL_DURATION_UNIT.getKey())));
            return d;
        }
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return this.name;
    }
}
