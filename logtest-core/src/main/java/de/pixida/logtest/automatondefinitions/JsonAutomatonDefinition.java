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

    private final File jsonFile;

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
        LOG.debug("Constructed loader from JSON file: {}", this.jsonFile.getAbsolutePath());
    }

    @Override
    public List<INodeDefinition> getNodes()
    {
        this.lazyLoadAutomaton();
        return new ArrayList<>(this.nodes);
    }

    @Override
    public List<IEdgeDefinition> getEdges()
    {
        this.lazyLoadAutomaton();
        return new ArrayList<>(this.edges);
    }

    @Override
    public String getOnLoad()
    {
        this.lazyLoadAutomaton();
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
        this.lazyLoadAutomaton();
        return this.comment;
    }

    @Override
    public String getScriptLanguage()
    {
        this.lazyLoadAutomaton();
        return this.scriptLanguage;
    }

    public void lazyLoadAutomaton()
    {
        if (!this.automatonLoaded)
        {
            try
            {
                final JSONObject root = new JSONObject(FileUtils.readFileToString(this.jsonFile, EXPECTED_CHARSET));
                this.onLoad = root.has("onLoad") ? root.getString("onLoad") : null;
                this.comment = root.has("comment") ? root.getString("comment") : null;
                this.scriptLanguage = root.has("scriptLanguage") ? root.getString("scriptLanguage") : null;
                this.extractNodesAndEdges(root);
                this.automatonLoaded = true;
            }
            catch (final IOException ioe)
            {
                throw new AutomatonLoadingException("Failed to read json file", ioe);
            }
            catch (final JSONException jsonEx)
            {
                throw new AutomatonLoadingException("Json data malformed", jsonEx);
            }
        }
    }

    private void extractNodesAndEdges(final JSONObject root)
    {
        this.nodes.clear();
        this.edges.clear();
        this.mapIdNode.clear();
        final JSONArray jsonNodes = root.getJSONArray("nodes");
        this.extractNodes(jsonNodes);
        this.extractEdges(jsonNodes);
    }

    public void extractNodes(final JSONArray jsonNodes)
    {
        for (int i = 0; i < jsonNodes.length(); i++)
        {
            final JSONObject node = jsonNodes.getJSONObject(i);
            final Set<INodeDefinition.Flag> flags = new HashSet<>();
            if (node.has("initial") && node.getBoolean("initial"))
            {
                flags.add(INodeDefinition.Flag.IS_INITIAL);
            }
            if (node.has("failure") && node.getBoolean("failure"))
            {
                flags.add(INodeDefinition.Flag.IS_FAILURE);
            }
            if (node.has("success") && node.getBoolean("success"))
            {
                flags.add(INodeDefinition.Flag.IS_SUCCESS);
            }
            final GenericNode newNode = new GenericNode(node.getString("id"), flags);
            this.nodes.add(newNode);
            if (node.has("onEnter"))
            {
                newNode.setOnEnter(node.getString("onEnter"));
            }
            if (node.has("onLeave"))
            {
                newNode.setOnLeave(node.getString("onLeave"));
            }
            if (node.has("successCheckExp"))
            {
                newNode.setSuccessCheckExp(node.getString("successCheckExp"));
            }
            newNode.setWait(node.has("wait") ? node.getBoolean("wait") : false);
            this.mapIdNode.put(node.getString("id"), newNode);
        }
    }

    public void extractEdges(final JSONArray jsonNodes)
    {
        for (int i = 0; i < jsonNodes.length(); i++)
        {
            final JSONObject node = jsonNodes.getJSONObject(i);
            if (node.has("outgoingEdges"))
            {
                final JSONArray outgoingEdges = node.getJSONArray("outgoingEdges");
                for (int j = 0; j < outgoingEdges.length(); j++)
                {
                    final JSONObject edge = outgoingEdges.getJSONObject(j);
                    final GenericNode source = this.mapIdNode.get(node.getString("id"));
                    final String destinationNodeId = edge.getString("destination");
                    final GenericNode destination = this.mapIdNode.get(destinationNodeId);
                    assert source != null;
                    if (destination == null)
                    {
                        throw new AutomatonLoadingException("Destination node not found: " + destinationNodeId);
                    }
                    final GenericEdge newEdge = new GenericEdge(edge.getString("id"), source, destination);
                    if (edge.has("onWalk"))
                    {
                        newEdge.setOnWalk(edge.getString("onWalk"));
                    }
                    this.retrieveEdgeConditions(edge, newEdge);
                    this.edges.add(newEdge);
                }
            }
        }
    }

    private void retrieveEdgeConditions(final JSONObject edge, final GenericEdge newEdge)
    {
        if (edge.has("regExp"))
        {
            newEdge.setRegExp(edge.getString("regExp"));
        }
        if (edge.has("checkExp"))
        {
            newEdge.setCheckExp(edge.getString("checkExp"));
        }
        if (edge.has("triggerAlways"))
        {
            newEdge.setTriggerAlways(edge.getBoolean("triggerAlways"));
        }
        if (edge.has("triggerOnEof"))
        {
            newEdge.setTriggerOnEof(edge.getBoolean("triggerOnEof"));
        }
        if (edge.has("requiredConditions"))
        {
            this.parseRequiredConditions(edge, newEdge);
        }
        newEdge.setTimeIntervalSinceLastMicrotransition(this.parseTimeInterval(edge, "timeIntervalSinceLastMicrotransition"));
        newEdge.setTimeIntervalSinceLastTransition(this.parseTimeInterval(edge, "timeIntervalSinceLastTransition"));
        newEdge.setTimeIntervalSinceAutomatonStart(this.parseTimeInterval(edge, "timeIntervalSinceAutomatonStart"));
        newEdge.setTimeIntervalForEvent(this.parseTimeInterval(edge, "timeIntervalForEvent"));
        final String channelKey = "channel";
        if (!edge.has(channelKey) || edge.isNull(channelKey))
        {
            newEdge.setChannel(ILogEntry.DEFAULT_CHANNEL);
        }
        else
        {
            newEdge.setChannel(edge.getString(channelKey));
        }
    }

    private void parseRequiredConditions(final JSONObject edge, final GenericEdge newEdge)
    {
        final String val = edge.getString("requiredConditions");
        if (val.equals("one"))
        {
            newEdge.setRequiredConditions(IEdgeDefinition.RequiredConditions.ONE);
        }
        else if (val.equals("all"))
        {
            newEdge.setRequiredConditions(IEdgeDefinition.RequiredConditions.ALL);
        }
        else
        {
            LOG.error("Invalid value for required conditions: {}", val);
            throw new AutomatonLoadingException("Invalid value for required conditions: " + val);
        }
    }

    private ITimeInterval parseTimeInterval(final JSONObject edge, final String key)
    {
        if (edge.has(key))
        {
            final JSONObject timeIntervalObject = edge.optJSONObject(key);
            if (timeIntervalObject != null)
            {
                final GenericTimeInterval timeInterval = new GenericTimeInterval();
                timeInterval.setMin(this.parseDuration(timeIntervalObject, "min"));
                timeInterval.setMax(this.parseDuration(timeIntervalObject, "max"));
                return timeInterval;
            }

            final String timeIntervalString = edge.optString(key);
            if (timeIntervalString != null)
            {
                return StringToGenericTimeIntervalConverter.convertStringToTimestamp(timeIntervalString);
            }
        }

        return null;
    }

    private GenericDuration parseDuration(final JSONObject timeIntervalObject, final String key)
    {
        final JSONObject durationObject = timeIntervalObject.getJSONObject(key);
        if (durationObject == null)
        {
            return null;
        }
        else
        {
            final GenericDuration d = new GenericDuration();
            d.setInclusive(durationObject.getBoolean("isInclusive"));
            d.setValue(durationObject.getString("value"));
            d.setUnit(StringToGenericTimeIntervalConverter.parseUnit(durationObject.getString("unit")));
            return d;
        }
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return this.jsonFile.getName();
    }
}
