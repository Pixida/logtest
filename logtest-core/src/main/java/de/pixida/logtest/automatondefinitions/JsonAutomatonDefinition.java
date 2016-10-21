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
import java.util.Locale;
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

    private static final String CONFIGURATION_KEY_DEPRECATED = "Use of configuration key '{}' is deprecated! Use '{}' instead.";

    private static final Logger LOG = LoggerFactory.getLogger(JsonAutomatonDefinition.class);

    private final File jsonFile;
    private final String jsonData;

    private boolean automatonLoaded = false;
    private final List<GenericNode> nodes = new ArrayList<>();
    private final List<GenericEdge> edges = new ArrayList<>();
    private final Map<String, GenericNode> mapIdNode = new HashMap<>();
    private final String name;
    private String description;
    private String onLoad;
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
    public String getDescription()
    {
        return this.description;
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
                this.description = this.loadOptStringAttributeFromJsonObjectWithDeprecatedAlternative(root, JsonKey.ROOT_DESCRIPTION,
                    JsonKey.ROOT_DESCRIPTION_DEPRECATED);
                this.onLoad = this.loadOptStringAttributeFromJsonObject(root, JsonKey.ROOT_ONLOAD);
                this.scriptLanguage = this.loadOptStringAttributeFromJsonObject(root, JsonKey.ROOT_SCRIPT_LANGUAGE);
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
            final GenericNode newNode = new GenericNode(node.getString("id"));
            newNode.setName(this.loadOptStringAttributeFromJsonObject(node, JsonKey.NODE_NAME));
            newNode.setDescription(this.loadOptStringAttributeFromJsonObjectWithDeprecatedAlternative(node, JsonKey.NODE_DESCRIPTION,
                JsonKey.NODE_DESCRIPTION_DEPRECATED));
            newNode.setType(this.loadOptEnumAttributeFromJsonObject(node, INodeDefinition.Type.class, JsonKey.NODE_TYPE));
            if (newNode.getType() == null)
            {
                newNode.setType(this.readTypeDefinedByDeprecatedFlags(node));
            }
            newNode.setOnEnter(this.loadOptStringAttributeFromJsonObject(node, JsonKey.NODE_ON_ENTER));
            newNode.setOnLeave(this.loadOptStringAttributeFromJsonObject(node, JsonKey.NODE_ON_LEAVE));
            newNode.setSuccessCheckExp(this.loadOptStringAttributeFromJsonObject(node, JsonKey.NODE_SUCCESS_CHECK_EXP));
            newNode.setWait(node.has(JsonKey.NODE_WAIT.getKey()) ? node.getBoolean(JsonKey.NODE_WAIT.getKey()) : false);
            this.nodes.add(newNode);
            this.mapIdNode.put(newNode.getId(), newNode);
        }
    }

    private INodeDefinition.Type readTypeDefinedByDeprecatedFlags(final JSONObject node)
    {
        final Set<INodeDefinition.Flag> flags = new HashSet<>();
        this.readDeprecatedFlag(node, JsonKey.NODE_INITIAL, INodeDefinition.Flag.IS_INITIAL, flags);
        this.readDeprecatedFlag(node, JsonKey.NODE_FAILURE, INodeDefinition.Flag.IS_FAILURE, flags);
        this.readDeprecatedFlag(node, JsonKey.NODE_SUCCESS, INodeDefinition.Flag.IS_SUCCESS, flags);
        if (flags.size() > 1)
        {
            throw new AutomatonLoadingException("A node can have only one type. Use property '" + JsonKey.NODE_TYPE.getKey()
                + "' instead of deprecated flag properties.");
        }
        if (flags.isEmpty())
        {
            return null;
        }
        if (flags.contains(INodeDefinition.Flag.IS_INITIAL))
        {
            return INodeDefinition.Type.INITIAL;
        }
        else if (flags.contains(INodeDefinition.Flag.IS_FAILURE))
        {
            return INodeDefinition.Type.FAILURE;
        }
        else if (flags.contains(INodeDefinition.Flag.IS_SUCCESS))
        {
            return INodeDefinition.Type.SUCCESS;
        }
        throw new RuntimeException("Internal error. No type for deprecated flag(s): " + flags.toString());
    }

    private void readDeprecatedFlag(final JSONObject node, final JsonKey key, final INodeDefinition.Flag mapToFlag,
        final Set<INodeDefinition.Flag> flags)
    {
        if (node.has(key.getKey()))
        {
            LOG.warn(CONFIGURATION_KEY_DEPRECATED, key.getKey(), JsonKey.NODE_TYPE.getKey());
            if (node.getBoolean(key.getKey()))
            {
                flags.add(mapToFlag);
            }
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
                    newEdge.setName(this.loadOptStringAttributeFromJsonObject(edge, JsonKey.EDGE_NAME));
                    newEdge.setDescription(this.loadOptStringAttributeFromJsonObjectWithDeprecatedAlternative(edge,
                        JsonKey.EDGE_DESCRIPTION, JsonKey.EDGE_DESCRIPTION_DEPRECATED));
                    newEdge.setOnWalk(this.loadOptStringAttributeFromJsonObject(edge, JsonKey.EDGE_ON_WALK));
                    this.retrieveEdgeConditions(edge, newEdge);
                    this.edges.add(newEdge);
                }
            }
        }
    }

    private void retrieveEdgeConditions(final JSONObject edge, final GenericEdge newEdge)
    {
        newEdge.setRegExp(this.loadOptStringAttributeFromJsonObject(edge, JsonKey.EDGE_REG_EXP));
        newEdge.setCheckExp(this.loadOptStringAttributeFromJsonObject(edge, JsonKey.EDGE_CHECK_EXP));
        newEdge.setTriggerAlways(this.loadOptBooleanAttributeFromJsonObject(edge, JsonKey.EDGE_TRIGGER_ALWAYS));
        newEdge.setTriggerOnEof(this.loadOptBooleanAttributeFromJsonObject(edge, JsonKey.EDGE_TRIGGER_ON_EOF));
        newEdge.setRequiredConditions(
            this.loadOptEnumAttributeFromJsonObject(edge, IEdgeDefinition.RequiredConditions.class, JsonKey.EDGE_REQUIRED_CONDITIONS));
        newEdge
            .setTimeIntervalSinceLastMicrotransition(this.parseTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_LAST_MICROTRANSITION));
        newEdge.setTimeIntervalSinceLastTransition(this.parseTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_LAST_TRANSITION));
        newEdge.setTimeIntervalSinceAutomatonStart(this.parseTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_SINCE_AUTOMATON_START));
        newEdge.setTimeIntervalForEvent(this.parseTimeInterval(edge, JsonKey.EDGE_TIME_INTERVAL_FOR_EVENT));
        newEdge.setChannel(this.loadOptStringAttributeFromJsonObject(edge, JsonKey.EDGE_CHANNEL));
        if (newEdge.getChannel() == null)
        {
            newEdge.setChannel(ILogEntry.DEFAULT_CHANNEL);
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

    private String loadOptStringAttributeFromJsonObject(final JSONObject jsonObject, final JsonKey key)
    {
        return jsonObject.has(key.getKey()) ? jsonObject.getString(key.getKey()) : null;
    }

    private Boolean loadOptBooleanAttributeFromJsonObject(final JSONObject jsonObject, final JsonKey key)
    {
        return jsonObject.has(key.getKey()) ? jsonObject.getBoolean(key.getKey()) : null;
    }

    private String loadOptStringAttributeFromJsonObjectWithDeprecatedAlternative(final JSONObject jsonObject, final JsonKey key,
        final JsonKey oldKey)
    {
        String value = this.loadOptStringAttributeFromJsonObject(jsonObject, key);
        if (value == null)
        {
            value = this.loadOptStringAttributeFromJsonObject(jsonObject, oldKey);
            if (value != null)
            {
                LOG.warn(CONFIGURATION_KEY_DEPRECATED, key.getKey(), oldKey.getKey());
            }
        }
        return value;
    }

    private <E extends Enum<E>> E loadOptEnumAttributeFromJsonObject(final JSONObject jsonObject, final Class<E> enumClz, final JsonKey key)
    {
        final String value = this.loadOptStringAttributeFromJsonObject(jsonObject, key);
        if (value == null)
        {
            return null;
        }
        final E[] enumConstants = enumClz.getEnumConstants();
        assert enumConstants != null; // E is an enum, guaranteed by declaration
        for (final E enumConstant : enumConstants)
        {
            if (value.toLowerCase(Locale.US).equals(enumConstant.toString().toLowerCase(Locale.US)))
            {
                return enumConstant;
            }
        }
        throw new AutomatonLoadingException("Invalid value for required conditions: " + value);
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return this.name;
    }
}
