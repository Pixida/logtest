/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;
import org.json.JSONObject;

import de.pixida.logtest.automatondefinitions.INodeDefinition;
import de.pixida.logtest.designer.commons.Icons;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

class AutomatonNode extends RectangularNode implements INodeDefinition
{
    private static final int Z_INDEX_AUTOMATON_NODES = 100;
    private static final int Z_INDEX_AUTOMATON_NODES_PROPERTIES = 120;
    private static final int Z_INDEX_AUTOMATON_NODES_CONNECTORS_TO_PROPERTIES = 90;

    private static final String JSON_KEY_RECT = "rect";

    private String id;
    private String name;
    private String description;
    private Type type;
    private String onEnter;
    private final PropertyNode onEnterPropertyNode;
    private String onLeave;
    private final PropertyNode onLeavePropertyNode;
    private String successCheckExp;
    private final PropertyNode successCheckExpPropertyNode;
    private boolean wait;
    private final PropertyNode descriptionPropertyNode;

    AutomatonNode(final Graph graph)
    {
        super(graph, ContentDisplayMode.ADJUST_RECT, Z_INDEX_AUTOMATON_NODES);

        this.onEnterPropertyNode = new PropertyNode(graph, this, "On Enter", Z_INDEX_AUTOMATON_NODES_PROPERTIES,
            Z_INDEX_AUTOMATON_NODES_CONNECTORS_TO_PROPERTIES);
        this.onLeavePropertyNode = new PropertyNode(graph, this, "On Leave", Z_INDEX_AUTOMATON_NODES_PROPERTIES,
            Z_INDEX_AUTOMATON_NODES_CONNECTORS_TO_PROPERTIES);
        this.successCheckExpPropertyNode = new PropertyNode(graph, this, "Check Success", Z_INDEX_AUTOMATON_NODES_PROPERTIES,
            Z_INDEX_AUTOMATON_NODES_CONNECTORS_TO_PROPERTIES);
        this.descriptionPropertyNode = new PropertyNode(graph, this, PropertyNode.WITHOUT_TITLE, Z_INDEX_AUTOMATON_NODES_PROPERTIES,
            Z_INDEX_AUTOMATON_NODES_CONNECTORS_TO_PROPERTIES);
        this.descriptionPropertyNode.setColor(Color.YELLOW.desaturate().desaturate().desaturate().brighter().brighter());
    }

    private Map<String, PropertyNode> getPropertyNodes()
    {
        final Map<String, PropertyNode> result = new HashMap<>();
        result.put("onEnter", this.onEnterPropertyNode);
        result.put("onLeave", this.onLeavePropertyNode);
        result.put("successCheckExp", this.successCheckExpPropertyNode);
        result.put("description", this.descriptionPropertyNode);
        return result;
    }

    void loadFromJson(final INodeDefinition node, final JSONObject nodeDesignerConfig)
    {
        Validate.notNull(node);
        Validate.notNull(nodeDesignerConfig);

        this.setId(node.getId());
        this.setName(node.getName());
        this.setDescription(node.getDescription());
        this.setType(node.getType());
        this.setOnEnter(node.getOnEnter());
        this.setOnLeave(node.getOnLeave());
        this.setSuccessCheckExp(node.getSuccessCheckExp());
        this.setWait(node.getWait());

        JSONObject rectDefinition = nodeDesignerConfig.optJSONObject(JSON_KEY_RECT);
        if (rectDefinition == null)
        {
            rectDefinition = new JSONObject();
        }
        this.loadDimensionsFromJson(rectDefinition);

        PropertyNode.loadAllPropertyNodesFromJson(this.getPropertyNodes(), nodeDesignerConfig);

        this.updateRect();
    }

    void saveToJson(final JSONObject nodeDesignerConfig)
    {
        Validate.notNull(nodeDesignerConfig);

        final JSONObject rectConfig = new JSONObject();
        this.saveDimensionsToJson(rectConfig);
        nodeDesignerConfig.put(JSON_KEY_RECT, rectConfig);

        PropertyNode.saveAllPropertyNodesToJson(this.getPropertyNodes(), nodeDesignerConfig);
    }

    @Override
    ContextMenu createContextMenu()
    {
        final ContextMenu cm = new ContextMenu();

        MenuItem mi = new MenuItem("Create edge from here");
        mi.setGraphic(Icons.getIconGraphics("bullet_go"));
        mi.setOnAction(event -> {
            final AutomatonEdgeBuilder newEdge = new AutomatonEdgeBuilder(this.getGraph());
            this.getGraph().startDrawingConnector(this, newEdge);
        });
        cm.getItems().add(mi);

        mi = new MenuItem("Delete state");
        mi.setGraphic(Icons.getIconGraphics("delete"));
        mi.setStyle("-fx-text-fill:#FF3030");
        mi.setOnAction(event -> {
            final Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Confirm");
            alert.setHeaderText("You are about to delete the state.");
            alert.setContentText("Do you want to continue?");
            alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> this.removeNodeAndEdges());
        });
        cm.getItems().add(mi);

        return cm;
    }

    @Override
    public Type getType()
    {
        return this.type;
    }

    void setType(final Type value)
    {
        this.type = value;
        this.showHideSuccessCheckExpDependingOnNodeType();
        this.updateRect();
    }

    private void showHideSuccessCheckExpDependingOnNodeType()
    {
        if (this.type != Type.SUCCESS)
        {
            this.successCheckExpPropertyNode.apply("");
        }
        else
        {
            this.successCheckExpPropertyNode.apply(this.successCheckExp);
        }
    }

    @Override
    public String getOnEnter()
    {
        return this.onEnter;
    }

    private void setOnEnter(final String value)
    {
        this.onEnter = this.onEnterPropertyNode.apply(value);
    }

    @Override
    public String getOnLeave()
    {
        return this.onLeave;
    }

    private void setOnLeave(final String value)
    {
        this.onLeave = this.onLeavePropertyNode.apply(value);
    }

    @Override
    public String getSuccessCheckExp()
    {
        return this.successCheckExp;
    }

    private void setSuccessCheckExp(final String value)
    {
        this.successCheckExp = this.successCheckExpPropertyNode.apply(value);
        this.showHideSuccessCheckExpDependingOnNodeType();
    }

    @Override
    public boolean getWait()
    {
        return this.wait;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    void setId(final String value)
    {
        this.id = value;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    private void setDescription(final String value)
    {
        this.description = this.descriptionPropertyNode.apply(value);
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    void setName(final String value)
    {
        this.name = value;
        this.setContent(this.name);
        this.updateRect();
    }

    @Override
    public Node getConfigFrame()
    {
        // TODO: Remove code redundancies; element creating methods have been created in class AutomatonEdge and should be centralized!
        final ConfigFrame cf = new ConfigFrame("State properties");

        final int nameInputLines = 1;
        final TextArea nameInput = new TextArea(this.name);
        nameInput.setPrefRowCount(nameInputLines);
        nameInput.setWrapText(true);
        nameInput.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            this.setName(newValue);
            this.getGraph().handleChange();
        });
        cf.addOption("Name", nameInput);

        final int descriptionInputLines = 3;
        this.createTextAreaInput(descriptionInputLines, cf, "Description", this.getDescription(),
            newValue -> this.setDescription(newValue));

        final VBox typeAttributes = new VBox();
        final ToggleGroup flagToggleGroup = new ToggleGroup();
        typeAttributes.getChildren().add(this.createRadioButtonForType(null, "None / Intermediate", flagToggleGroup));
        typeAttributes.getChildren().add(this.createRadioButtonForType(Type.INITIAL, "Initial", flagToggleGroup));
        final RadioButton successOption = this.createRadioButtonForType(Type.SUCCESS, "Success", flagToggleGroup);
        typeAttributes.getChildren().add(successOption);
        typeAttributes.getChildren().add(this.createRadioButtonForType(Type.FAILURE, "Failure", flagToggleGroup));
        cf.addOption("Type", typeAttributes);

        final CheckBox waitCheckBox = new CheckBox("Active");
        waitCheckBox.setSelected(this.wait);
        waitCheckBox.setOnAction(event -> {
            this.setWait(waitCheckBox.isSelected());
            this.getGraph().handleChange();
        });
        cf.addOption("Wait", waitCheckBox);

        final int expressionInputLines = 1;
        final TextArea successCheckExpInput = new TextArea(this.successCheckExp);
        successCheckExpInput.setStyle("-fx-font-family: monospace");
        successCheckExpInput.setPrefRowCount(expressionInputLines);
        successCheckExpInput.setWrapText(false);
        successCheckExpInput.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            this.setSuccessCheckExp(newValue);
            this.getGraph().handleChange();
        });
        successCheckExpInput.disableProperty().bind(successOption.selectedProperty().not());
        cf.addOption("Script expression to verify if node is successful", successCheckExpInput);

        this.createEnterAndLeaveScriptConfig(cf);

        return cf;
    }

    public void createEnterAndLeaveScriptConfig(final ConfigFrame cf)
    {
        final int scriptInputLines = 3;
        final TextArea onEnterScriptInput = new TextArea(this.onEnter);
        onEnterScriptInput.setStyle("-fx-font-family: monospace");
        onEnterScriptInput.setPrefRowCount(scriptInputLines);
        onEnterScriptInput.setWrapText(false);
        onEnterScriptInput.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            this.setOnEnter(newValue);
            this.getGraph().handleChange();
        });
        cf.addOption("When entering state, run script", onEnterScriptInput);

        final TextArea onLeaveScriptInput = new TextArea(this.onLeave);
        onLeaveScriptInput.setStyle("-fx-font-family: monospace");
        onLeaveScriptInput.setPrefRowCount(scriptInputLines);
        onLeaveScriptInput.setWrapText(false);
        onLeaveScriptInput.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            this.setOnLeave(newValue);
            this.getGraph().handleChange();
        });
        cf.addOption("When leaving state, run script", onLeaveScriptInput);
    }

    protected void createTextAreaInput(final int numLines, final ConfigFrame cf, final String propertyName, final String initialValue,
        final Consumer<String> applyValue)
    {
        final TextArea inputField = new TextArea(initialValue);
        inputField.setPrefRowCount(numLines);
        inputField.setWrapText(true);
        inputField.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            applyValue.accept(newValue);
            this.getGraph().handleChange();
        });
        cf.addOption(propertyName, inputField);
    }

    private void setWait(final boolean value)
    {
        this.wait = value;
        this.updateRect();
    }

    private RadioButton createRadioButtonForType(final Type aType, final String title, final ToggleGroup toggleGroup)
    {
        final RadioButton result = new RadioButton(title);
        result.setToggleGroup(toggleGroup);
        result.setSelected(this.type == aType);
        result.setOnAction(event -> {
            this.setType(aType);
        });
        return result;
    }

    private void updateRect()
    {
        String title = "";
        if (this.type == Type.INITIAL)
        {
            this.setColor(Color.BLUE.desaturate().desaturate().desaturate().desaturate().brighter().brighter());
            title += "INITIAL ";
        }
        else if (this.type == Type.SUCCESS)
        {
            this.setColor(Color.GREEN.desaturate().desaturate().desaturate().desaturate().brighter().brighter());
            title += "SUCCESS ";
        }
        else if (this.type == Type.FAILURE)
        {
            this.setColor(Color.RED.desaturate().desaturate().desaturate().desaturate().brighter().brighter());
            title += "FAILURE ";
        }
        else
        {
            this.setColor(Color.LIGHTGREY);
        }
        title += "State";
        if (this.wait)
        {
            title += " | wait!";
        }
        this.setTitle(title);
    }
}
