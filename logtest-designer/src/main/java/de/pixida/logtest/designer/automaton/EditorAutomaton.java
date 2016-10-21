/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONException;
import org.json.JSONObject;

import de.pixida.logtest.automatondefinitions.AutomatonDefinitionToJsonConverter;
import de.pixida.logtest.automatondefinitions.IAutomatonDefinition;
import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.automatondefinitions.INodeDefinition;
import de.pixida.logtest.automatondefinitions.JsonAutomatonDefinition;
import de.pixida.logtest.designer.automaton.RectangularNode.ContentDisplayMode;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

class EditorAutomaton implements IAutomatonDefinition
{
    private static final String JSON_KEY_DESIGNER = "designer";
    private static final String JSON_KEY_NODES = "nodes";
    private static final String JSON_KEY_EDGES = "edges";
    private static final String JSON_KEY_DESCRIPTION = "description";

    private static final int Z_INDEX_AUTOMATON_PROPERTIES = 1;

    private String onLoad;
    private String description;
    private final RectangularNode descriptionNode;
    private String scriptLanguage;

    private final Graph graph;

    EditorAutomaton(final Graph aGraph)
    {
        Validate.notNull(aGraph);
        this.graph = aGraph;

        this.descriptionNode = new RectangularNode(this.graph, ContentDisplayMode.ADJUST_RECT, Z_INDEX_AUTOMATON_PROPERTIES);
        this.descriptionNode.setColor(Color.YELLOW.desaturate().desaturate().desaturate().brighter().brighter());
        this.descriptionNode.setTitle("Abstract");
        this.descriptionNode.hide();
        this.graph.addObject(this.descriptionNode);
    }

    @Override
    public List<? extends INodeDefinition> getNodes()
    {
        return this.graph.getAllNodesByClass(INodeDefinition.class);
    }

    @Override
    public List<? extends IEdgeDefinition> getEdges()
    {
        return this.graph.getAllNodesByClass(IEdgeDefinition.class);
    }

    @Override
    public String getOnLoad()
    {
        return this.onLoad;
    }

    @Override
    public String getDisplayName()
    {
        return "Editor Automaton";
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

    Node getConfigFrame()
    {
        final ConfigFrame cf = new ConfigFrame("Automaton properties");

        final int descriptionInputLines = 8;
        final TextArea descriptionInput = new TextArea(this.description);
        descriptionInput.setPrefRowCount(descriptionInputLines);
        descriptionInput.setWrapText(true);
        descriptionInput.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            this.setDescription(newValue);
            this.graph.handleChange();
        });
        cf.addOption("Description", descriptionInput);

        final ObservableList<String> options = FXCollections.observableArrayList("JavaScript", "Python");
        final ChoiceBox<String> scriptLanguageInput = new ChoiceBox<>(options);
        scriptLanguageInput.setValue(
            StringUtils.isBlank(this.scriptLanguage) || !options.contains(this.scriptLanguage) ? options.get(0) : this.scriptLanguage);
        scriptLanguageInput.getSelectionModel().selectedIndexProperty()
            .addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
                EditorAutomaton.this.scriptLanguage = scriptLanguageInput.getItems().get((Integer) newValue);
                this.graph.handleChange();
            });
        cf.addOption("Script language", scriptLanguageInput);

        return cf;
    }

    @Override
    public void load()
    {
        // Loading is not done using this method, e.g. the automaton must be loaded before this method is called. Otherwise, it will be
        // empty (no nodes, edges, etc.)
    }

    void loadFromFile(final File src)
    {
        JSONObject rawJson = null;
        try
        {
            rawJson = new JSONObject(FileUtils.readFileToString(src, JsonAutomatonDefinition.EXPECTED_CHARSET));
        }
        catch (final JSONException e)
        {
            throw new RuntimeException("Invalid JSON data - maybe not an automaton definition?\n" + e.getMessage());
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Failed to read file: " + src.getAbsolutePath(), e);
        }

        final JsonAutomatonDefinition def = new JsonAutomatonDefinition(src);
        def.load();
        this.onLoad = def.getOnLoad();
        this.setDescription(def.getDescription());
        this.scriptLanguage = def.getScriptLanguage();

        final JSONObject designerConfig = rawJson.optJSONObject(JSON_KEY_DESIGNER);

        final JSONObject nodesDesignerConfig = designerConfig == null ? null : designerConfig.optJSONObject(JSON_KEY_NODES);
        final Map<String, AutomatonNode> mapIdToNode = this.createNodes(def, nodesDesignerConfig);

        final JSONObject edgesDesignerConfig = designerConfig == null ? null : designerConfig.optJSONObject(JSON_KEY_EDGES);
        this.createEdges(def, mapIdToNode, edgesDesignerConfig);

        JSONObject descriptionConfig = designerConfig == null ? null : designerConfig.optJSONObject(JSON_KEY_DESCRIPTION);
        if (descriptionConfig == null)
        {
            descriptionConfig = new JSONObject();
        }
        this.descriptionNode.loadDimensionsFromJson(descriptionConfig);

        // HACK: For some reason, the edges are not aligned correctly at this point. Fix this...
        this.graph.getAllNodesByClass(BaseEdge.class).forEach(connector -> connector.realign());
    }

    void setDescription(final String value)
    {
        final String oldValue = this.description;
        if (StringUtils.isBlank(oldValue) && StringUtils.isNotBlank(value))
        {
            final double defaultXY = 10d;
            this.descriptionNode.setPosition(new Point2D(defaultXY, defaultXY));
            this.descriptionNode.show();
        }
        if (StringUtils.isBlank(value))
        {
            this.descriptionNode.hide();
        }
        this.description = value;
        this.descriptionNode.setContent(this.description);
    }

    private Map<String, AutomatonNode> createNodes(final JsonAutomatonDefinition def, final JSONObject nodesDesignerConfig)
    {
        final Map<String, AutomatonNode> mapIdToNode = new HashMap<>();
        for (final INodeDefinition node : def.getNodes())
        {
            Validate.notNull(node.getId());
            JSONObject nodeDesignerConfig = nodesDesignerConfig == null ? null : nodesDesignerConfig.optJSONObject(node.getId());
            if (nodeDesignerConfig == null)
            {
                nodeDesignerConfig = new JSONObject();
            }

            final AutomatonNode newNode = new AutomatonNode(this.graph);
            newNode.loadFromJson(node, nodeDesignerConfig);
            this.graph.addObject(newNode);
            mapIdToNode.put(newNode.getId(), newNode);
        }
        return mapIdToNode;
    }

    private void createEdges(final JsonAutomatonDefinition def, final Map<String, AutomatonNode> mapIdToNode,
        final JSONObject edgesDesignerConfig)
    {
        for (final IEdgeDefinition edge : def.getEdges())
        {
            Validate.notNull(edge.getSource());
            Validate.notNull(edge.getDestination());
            Validate.isTrue(mapIdToNode.containsKey(edge.getSource().getId()));
            Validate.isTrue(mapIdToNode.containsKey(edge.getDestination().getId()));

            Validate.notNull(edge.getId());
            JSONObject edgeDesignerConfig = edgesDesignerConfig == null ? null : edgesDesignerConfig.optJSONObject(edge.getId());
            if (edgeDesignerConfig == null)
            {
                edgeDesignerConfig = new JSONObject();
            }

            final AutomatonEdgeBuilder edgeBuilder = new AutomatonEdgeBuilder(this.graph);
            final AutomatonNode sourceNode = mapIdToNode.get(edge.getSource().getId());
            final AutomatonNode targetNode = mapIdToNode.get(edge.getDestination().getId());
            edgeBuilder.attach(sourceNode, targetNode);
            final AutomatonEdge newEdge = edgeBuilder.getCreatedEdge();
            newEdge.loadFromJson(edge, edgeDesignerConfig);
        }
    }

    void saveToFile(final File dest)
    {
        this.assignArbitraryIdsToNodesAndEdges();

        final JSONObject root = AutomatonDefinitionToJsonConverter.convert(this);

        final JSONObject designerConfig = new JSONObject();
        root.put(JSON_KEY_DESIGNER, designerConfig);

        final JSONObject nodesDesignerConfig = new JSONObject();
        designerConfig.put(JSON_KEY_NODES, nodesDesignerConfig);
        for (final AutomatonNode node : this.graph.getAllNodesByClass(AutomatonNode.class))
        {
            final JSONObject nodeDesignerConfig = new JSONObject();
            node.saveToJson(nodeDesignerConfig);
            nodesDesignerConfig.put(node.getId(), nodeDesignerConfig);
        }

        final JSONObject edgesDesignerConfig = new JSONObject();
        designerConfig.put(JSON_KEY_EDGES, edgesDesignerConfig);
        for (final AutomatonEdge edge : this.graph.getAllNodesByClass(AutomatonEdge.class))
        {
            final JSONObject edgeDesignerConfig = new JSONObject();
            edge.saveToJson(edgeDesignerConfig);
            edgesDesignerConfig.put(edge.getId(), edgeDesignerConfig);
        }

        final JSONObject descriptionConfig = new JSONObject();
        this.descriptionNode.saveDimensionsToJson(descriptionConfig);
        designerConfig.put(JSON_KEY_DESCRIPTION, descriptionConfig);

        final int indentSpaces = 4;
        try
        {
            FileUtils.write(dest, root.toString(indentSpaces), JsonAutomatonDefinition.EXPECTED_CHARSET);
        }
        catch (final JSONException e)
        {
            throw new RuntimeException("JSON format exception while writing file", e);
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Error while saving file: " + e.getMessage());
        }
    }

    private void assignArbitraryIdsToNodesAndEdges()
    {
        final AtomicInteger idCounter = new AtomicInteger();
        this.graph.getAllNodesByClass(AutomatonNode.class).forEach(node -> node.setId(String.valueOf(idCounter.incrementAndGet())));
        this.graph.getAllNodesByClass(AutomatonEdge.class).forEach(edge -> edge.setId(String.valueOf(idCounter.incrementAndGet())));
    }

    void dispose()
    {
        this.graph.removeAllObjects();
    }
}
