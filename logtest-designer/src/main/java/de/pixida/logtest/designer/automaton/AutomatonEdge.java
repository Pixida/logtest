/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONObject;

import de.pixida.logtest.automatondefinitions.GenericDuration;
import de.pixida.logtest.automatondefinitions.GenericTimeInterval;
import de.pixida.logtest.automatondefinitions.IDuration;
import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.automatondefinitions.INodeDefinition;
import de.pixida.logtest.automatondefinitions.ITimeInterval;
import de.pixida.logtest.automatondefinitions.JsonTimeUnit;
import de.pixida.logtest.designer.commons.Icons;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

class AutomatonEdge extends CircularNode implements IEdgeDefinition
{
    private static final String JSON_KEY_EDGE_POS_X = "x";
    private static final String JSON_KEY_EDGE_POS_Y = "y";

    private static final int Z_INDEX_AUTOMATON_EDGE_NODES = 160;
    private static final int Z_INDEX_AUTOMATON_EDGES_PROPERTIES = 120;
    private static final int Z_INDEX_AUTOMATON_EDGES_CONNECTORS_TO_PROPERTIES = 90;

    private static class DurationForEditing
    {
        private static final String DEFAULT_VALUE = "";
        private static final TimeUnit DEFAULT_UNIT = TimeUnit.MILLISECONDS;
        private static final boolean DEFAULT_IS_INCLUSIVE = true;

        private String value;
        private TimeUnit unit;
        private boolean inclusive;

        DurationForEditing()
        {
            this.fromDuration(null);
        }

        boolean isInclusive()
        {
            return this.inclusive;
        }

        void setInclusive(final boolean aValue)
        {
            this.inclusive = aValue;
        }

        String getValue()
        {
            return this.value;
        }

        void setValue(final String aValue)
        {
            this.value = aValue;
        }

        TimeUnit getUnit()
        {
            return this.unit;
        }

        void setUnit(final TimeUnit aValue)
        {
            this.unit = aValue;
        }

        void fromDuration(final IDuration duration)
        {
            if (duration == null)
            {
                this.value = DEFAULT_VALUE;
                this.unit = DEFAULT_UNIT;
                this.inclusive = DEFAULT_IS_INCLUSIVE;
            }
            else
            {
                this.value = duration.getValue();
                this.unit = duration.getUnit();
                this.inclusive = duration.isInclusive();
            }
        }

        IDuration toDuration()
        {
            GenericDuration result = null;
            if (StringUtils.isNotBlank(this.value))
            {
                result = new GenericDuration();
                result.setValue(this.value);
                result.setUnit(this.unit);
                result.setInclusive(this.inclusive);
            }
            return result;
        }
    }

    private static class TimeIntervalForEditing
    {
        private final String name;
        private final DurationForEditing min = new DurationForEditing();
        private final DurationForEditing max = new DurationForEditing();

        TimeIntervalForEditing(final String aName)
        {
            this.name = aName;
        }

        DurationForEditing getMinDuration()
        {
            return this.min;
        }

        DurationForEditing getMaxDuration()
        {
            return this.max;
        }

        String getName()
        {
            return this.name;
        }

        ITimeInterval toTimeInterval()
        {
            final GenericTimeInterval result = new GenericTimeInterval();
            result.setMin(this.min.toDuration());
            result.setMax(this.max.toDuration());
            if (result.getMin() == null && result.getMax() == null)
            {
                return null;
            }
            return result;
        }

        void fromTimeInterval(final ITimeInterval value)
        {
            if (value == null)
            {
                this.min.fromDuration(null);
                this.max.fromDuration(null);
            }
            else
            {
                this.min.fromDuration(value.getMin());
                this.max.fromDuration(value.getMax());
            }
        }

        String toPropertyString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(this.name);
            sb.append(": ");
            if (this.min.toDuration() == null)
            {
                sb.append("(-\u221E");
            }
            else
            {
                sb.append(this.min.inclusive ? '[' : '(');
                sb.append(this.min.getValue());
                sb.append(this.getUnitString(this.min.getUnit()));
            }
            sb.append("; ");
            if (this.max.toDuration() == null)
            {
                sb.append("\u221E)");
            }
            else
            {
                sb.append(this.max.getValue());
                sb.append(this.getUnitString(this.max.getUnit()));
                sb.append(this.max.inclusive ? ']' : ')');
            }
            return sb.toString();
        }

        private String getUnitString(final TimeUnit unit)
        {
            final String result = JsonTimeUnit.convertTimeUnitToString(unit);
            if (result == null)
            {
                return "<?>";
            }
            else
            {
                return result;
            }
        }
    }

    private enum Monospace
    {
        YES,
        NO
    }

    private String id;
    private String name;
    private String description;
    private String regExp;
    private final PropertyNode regExpPropertyNode;
    private String checkExp;
    private final PropertyNode checkExpPropertyNode;
    private Boolean triggerAlways;
    private String onWalk;
    private final PropertyNode onWalkPropertyNode;
    private Boolean triggerOnEof;
    private final PropertyNode triggerOnEofPropertyNode;
    private RequiredConditions requiredCondition;
    private final TimeIntervalForEditing timeIntervalSinceLastMicrotransition = new TimeIntervalForEditing("Since last microtransition");
    private final TimeIntervalForEditing timeIntervalSinceLastTransition = new TimeIntervalForEditing("Since last transition");
    private final TimeIntervalForEditing timeIntervalSinceAutomatonStart = new TimeIntervalForEditing("Since automaton start");
    private final TimeIntervalForEditing timeIntervalForEvent = new TimeIntervalForEditing("Event timestamp");
    private final PropertyNode timingConditionsPropertyNode;
    private String channel;
    private final PropertyNode channelPropertyNode;
    private final PropertyNode descriptionPropertyNode;

    AutomatonEdge(final Graph graph)
    {
        super(graph, Z_INDEX_AUTOMATON_EDGE_NODES);

        this.regExpPropertyNode = new PropertyNode(graph, this, "RegExp", Z_INDEX_AUTOMATON_EDGES_PROPERTIES,
            Z_INDEX_AUTOMATON_EDGES_CONNECTORS_TO_PROPERTIES);
        this.checkExpPropertyNode = new PropertyNode(graph, this, "Check Expression", Z_INDEX_AUTOMATON_EDGES_PROPERTIES,
            Z_INDEX_AUTOMATON_EDGES_CONNECTORS_TO_PROPERTIES);
        this.triggerOnEofPropertyNode = new PropertyNode(graph, this, "EOF", Z_INDEX_AUTOMATON_EDGES_PROPERTIES,
            Z_INDEX_AUTOMATON_EDGES_CONNECTORS_TO_PROPERTIES);
        this.onWalkPropertyNode = new PropertyNode(graph, this, "On Walk", Z_INDEX_AUTOMATON_EDGES_PROPERTIES,
            Z_INDEX_AUTOMATON_EDGES_CONNECTORS_TO_PROPERTIES);
        this.channelPropertyNode = new PropertyNode(graph, this, "Channel", Z_INDEX_AUTOMATON_EDGES_PROPERTIES,
            Z_INDEX_AUTOMATON_EDGES_CONNECTORS_TO_PROPERTIES);
        this.descriptionPropertyNode = new PropertyNode(graph, this, PropertyNode.WITHOUT_TITLE, Z_INDEX_AUTOMATON_EDGES_PROPERTIES,
            Z_INDEX_AUTOMATON_EDGES_CONNECTORS_TO_PROPERTIES);
        this.descriptionPropertyNode.setColor(Color.YELLOW.desaturate().desaturate().desaturate().brighter().brighter());
        this.timingConditionsPropertyNode = new PropertyNode(graph, this, "Timing Conditions", Z_INDEX_AUTOMATON_EDGES_PROPERTIES,
            Z_INDEX_AUTOMATON_EDGES_CONNECTORS_TO_PROPERTIES);
    }

    private Map<String, PropertyNode> getPropertyNodes()
    {
        final Map<String, PropertyNode> result = new HashMap<>();
        result.put("regExpPropertyNode", this.regExpPropertyNode);
        result.put("checkExpPropertyNode", this.checkExpPropertyNode);
        result.put("triggerOnEofPropertyNode", this.triggerOnEofPropertyNode);
        result.put("onWalkPropertyNode", this.onWalkPropertyNode);
        result.put("channelPropertyNode", this.channelPropertyNode);
        result.put("descriptionPropertyNode", this.descriptionPropertyNode);
        result.put("timingConditionsPropertyNode", this.timingConditionsPropertyNode);
        return result;
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
    public String getName()
    {
        return this.name;
    }

    private void setName(final String value)
    {
        this.name = value;
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
    ContextMenu createContextMenu()
    {
        final ContextMenu cm = new ContextMenu();

        final MenuItem mi = new MenuItem("Delete edge");
        mi.setGraphic(Icons.getIconGraphics("delete"));
        mi.setStyle("-fx-text-fill:red");
        mi.setOnAction(event -> {
            final Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Confirm");
            alert.setHeaderText("You are about to delete the edge.");
            alert.setContentText("Do you want to continue?");
            alert.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> this.removeNodeAndEdges());
        });
        cm.getItems().add(mi);

        return cm;
    }

    @Override
    public Node getConfigFrame()
    {
        final ConfigFrame cf = new ConfigFrame("Edge properties");

        final int nameInputLines = 1;
        this.createTextAreaInput(nameInputLines, cf, "Name", this.getName(), newValue -> this.setName(newValue), Monospace.NO);

        final int descriptionInputLines = 2;
        this.createTextAreaInput(descriptionInputLines, cf, "Description", this.getDescription(),
            newValue -> this.setDescription(newValue), Monospace.NO);

        this.createTextFieldInput(cf, "Regular Expression", this.getRegExp(), newValue -> this.setRegExp(newValue), Monospace.YES);

        final int scriptInputLines = 3;
        this.createTextAreaInput(scriptInputLines, cf, "Check Expression", this.getCheckExp(), newValue -> this.setCheckExp(newValue),
            Monospace.YES);

        final VBox specialTriggers = new VBox();
        specialTriggers.getChildren()
            .add(this.createCheckBoxInput("Always Trigger", this.getTriggerAlways(), newValue -> this.setTriggerAlways(newValue)));
        specialTriggers.getChildren()
            .add(this.createCheckBoxInput("Trigger On EOF", this.getTriggerOnEof(), newValue -> this.setTriggerOnEof(newValue)));
        cf.addOption("Special Triggers", specialTriggers);

        this.createTextAreaInput(scriptInputLines, cf, "On Walk", this.getOnWalk(), newValue -> this.setOnWalk(newValue), Monospace.YES);

        final VBox requiredConditionsAndOrOr = new VBox();
        final ToggleGroup tg = new ToggleGroup();
        requiredConditionsAndOrOr.getChildren()
            .add(this.createRadioButtonInput(tg, "All (AND)",
                this.getRequiredConditions() != null && this.getRequiredConditions() == RequiredConditions.ALL,
                newValue -> {
                    if (newValue)
                    {
                        this.setRequiredConditions(RequiredConditions.ALL);
                    }
                }));
        requiredConditionsAndOrOr.getChildren()
            .add(this.createRadioButtonInput(tg, "One (OR)",
                this.getRequiredConditions() != null && this.getRequiredConditions() == RequiredConditions.ONE,
                newValue -> {
                    if (newValue)
                    {
                        this.setRequiredConditions(RequiredConditions.ONE);
                    }
                }));
        cf.addOption("Required Conditions", requiredConditionsAndOrOr);

        this.createTextFieldInput(cf, "Channel", this.getChannel(), newValue -> this.setChannel(newValue), Monospace.NO);

        this.createTimeIntervalInput(cf, this.timeIntervalSinceLastMicrotransition);
        this.createTimeIntervalInput(cf, this.timeIntervalSinceLastTransition);
        this.createTimeIntervalInput(cf, this.timeIntervalSinceAutomatonStart);
        this.createTimeIntervalInput(cf, this.timeIntervalForEvent);

        return cf;
    }

    private void createTimeIntervalInput(final ConfigFrame cf, final TimeIntervalForEditing timeInterval)
    {
        final GridPane gp = new GridPane();
        final ColumnConstraints column1 = new ColumnConstraints();
        final ColumnConstraints column2 = new ColumnConstraints();
        final ColumnConstraints column3 = new ColumnConstraints();
        column3.setHgrow(Priority.SOMETIMES);
        final ColumnConstraints column4 = new ColumnConstraints();
        gp.getColumnConstraints().addAll(column1, column2, column3, column4);
        this.createDurationInput(gp, true, timeInterval);
        this.createDurationInput(gp, false, timeInterval);
        cf.addOption(timeInterval.getName(), gp);
    }

    private void createDurationInput(final GridPane gp, final boolean isMin, final TimeIntervalForEditing timeInterval)
    {
        final DurationForEditing duration = isMin ? timeInterval.getMinDuration() : timeInterval.getMaxDuration();

        final int targetRow = isMin ? 0 : 1;
        int column = 0;

        final Text fromOrTo = new Text((isMin ? "Min" : "Max") + ": ");
        gp.add(fromOrTo, column++, targetRow);

        final String inclusive = "Inclusive";
        final String exclusive = "Exclusive";
        final ChoiceBox<String> intervalBorderInput = new ChoiceBox<>(FXCollections.observableArrayList(inclusive, exclusive));
        intervalBorderInput.setValue(duration.isInclusive() ? inclusive : exclusive);
        intervalBorderInput.getSelectionModel().selectedIndexProperty()
            .addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
                duration.setInclusive(0 == (Integer) newValue);
                this.timingConditionsUpdated();
                this.getGraph().handleChange();
            });
        gp.add(intervalBorderInput, column++, targetRow);

        final TextField valueInput = new TextField(duration.getValue());
        valueInput.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            duration.setValue(newValue);
            this.timingConditionsUpdated();
            this.getGraph().handleChange();
        });
        gp.add(valueInput, column++, targetRow);

        final String currentChoice = JsonTimeUnit.convertTimeUnitToString(duration.getUnit());
        final ChoiceBox<String> timeUnitInput = new ChoiceBox<>(FXCollections.observableArrayList(JsonTimeUnit.getListOfPossibleNames()));
        timeUnitInput.setValue(currentChoice);
        timeUnitInput.getSelectionModel().selectedIndexProperty()
            .addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
                duration.setUnit(JsonTimeUnit.indexToTimeUnit((Integer) newValue));
                this.timingConditionsUpdated();
                this.getGraph().handleChange();
            });
        gp.add(timeUnitInput, column++, targetRow);
    }

    private Node createCheckBoxInput(final String propertyName, final Boolean initialValue, final Consumer<Boolean> applyValue)
    {
        final CheckBox result = new CheckBox(propertyName);
        result.setSelected(BooleanUtils.isTrue(initialValue));
        result.setOnAction(event -> {
            applyValue.accept(result.isSelected());
            this.getGraph().handleChange();
        });
        return result;
    }

    private Node createRadioButtonInput(final ToggleGroup tg, final String propertyName, final boolean isInitiallySelected,
        final Consumer<Boolean> applyValue)
    {
        final RadioButton result = new RadioButton(propertyName);
        result.setToggleGroup(tg);
        result.setSelected(isInitiallySelected);
        result.setOnAction(event -> {
            applyValue.accept(result.isSelected());
            this.getGraph().handleChange();
        });
        return result;
    }

    protected void createTextAreaInput(final int numLines, final ConfigFrame cf, final String propertyName, final String initialValue,
        final Consumer<String> applyValue, final Monospace monospace)
    {
        final TextArea inputField = new TextArea(initialValue);
        this.setMonospaceIfDesired(monospace, inputField);
        inputField.setPrefRowCount(numLines);
        inputField.setWrapText(true);
        inputField.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            applyValue.accept(newValue);
            this.getGraph().handleChange();
        });
        cf.addOption(propertyName, inputField);
    }

    protected void createTextFieldInput(final ConfigFrame cf, final String propertyName, final String initialValue,
        final Consumer<String> applyValue, final Monospace monospace)
    {
        final TextField inputField = new TextField(initialValue);
        this.setMonospaceIfDesired(monospace, inputField);
        inputField.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            applyValue.accept(newValue);
            this.getGraph().handleChange();
        });
        cf.addOption(propertyName, inputField);
    }

    private void setMonospaceIfDesired(final Monospace monospace, final TextInputControl inputField)
    {
        if (monospace == Monospace.YES)
        {
            inputField.setStyle("-fx-font-family: monospace");
        }
    }

    @Override
    public INodeDefinition getSource()
    {
        final List<AutomatonNode> sources = this.getPreviousNodesFilteredByType(AutomatonNode.class);
        Validate.isTrue(sources.size() == 1);
        Validate.isTrue(sources.get(0) instanceof INodeDefinition);
        return sources.get(0);
    }

    @Override
    public INodeDefinition getDestination()
    {
        final List<AutomatonNode> destinations = this.getNextNodesFilteredByType(AutomatonNode.class);
        Validate.isTrue(destinations.size() == 1);
        Validate.isTrue(destinations.get(0) instanceof INodeDefinition);
        return destinations.get(0);
    }

    @Override
    public String getRegExp()
    {
        return this.regExp;
    }

    private void setRegExp(final String value)
    {
        this.regExp = this.regExpPropertyNode.apply(value);
    }

    @Override
    public Boolean getTriggerAlways()
    {
        return this.triggerAlways;
    }

    public void setTriggerAlways(final Boolean value)
    {
        this.triggerAlways = value;
        if (BooleanUtils.isTrue(this.triggerAlways))
        {
            this.setColor(Color.GREEN.desaturate().desaturate().desaturate().brighter().brighter());
        }
        else
        {
            this.setColor(CircularNode.DEFAULT_COLOR);
        }
    }

    @Override
    public String getCheckExp()
    {
        return this.checkExp;
    }

    private void setCheckExp(final String value)
    {
        this.checkExp = this.checkExpPropertyNode.apply(value);
    }

    @Override
    public String getOnWalk()
    {
        return this.onWalk;
    }

    private void setOnWalk(final String value)
    {
        this.onWalk = this.onWalkPropertyNode.apply(value);
    }

    @Override
    public Boolean getTriggerOnEof()
    {
        return this.triggerOnEof;
    }

    private void setTriggerOnEof(final Boolean value)
    {
        this.triggerOnEof = this.triggerOnEofPropertyNode.apply(value);
    }

    @Override
    public RequiredConditions getRequiredConditions()
    {
        return this.requiredCondition;
    }

    private void setRequiredConditions(final RequiredConditions value)
    {
        // TODO: Mirror this on the UI (e.g. a DOT for AND and a PLUS for OR inside the circle of this edge)
        this.requiredCondition = value;
    }

    @Override
    public ITimeInterval getTimeIntervalSinceLastMicrotransition()
    {
        return this.timeIntervalSinceLastMicrotransition.toTimeInterval();
    }

    @Override
    public ITimeInterval getTimeIntervalSinceLastTransition()
    {
        return this.timeIntervalSinceLastTransition.toTimeInterval();
    }

    @Override
    public ITimeInterval getTimeIntervalSinceAutomatonStart()
    {
        return this.timeIntervalSinceAutomatonStart.toTimeInterval();
    }

    @Override
    public ITimeInterval getTimeIntervalForEvent()
    {
        return this.timeIntervalForEvent.toTimeInterval();
    }

    private void timingConditionsUpdated()
    {
        final String textToDisplay = Arrays.asList(this.timeIntervalSinceLastMicrotransition,
            this.timeIntervalSinceLastTransition,
            this.timeIntervalSinceAutomatonStart,
            this.timeIntervalForEvent)
            .stream()
            .filter(interval -> interval.toTimeInterval() != null)
            .map(interval -> interval.toPropertyString())
            .collect(Collectors.joining("\n"));
        this.timingConditionsPropertyNode.apply(textToDisplay);
    }

    @Override
    public String getChannel()
    {
        return this.channel;
    }

    private void setChannel(final String value)
    {
        this.channel = this.channelPropertyNode.apply(value);
    }

    void loadFromJson(final IEdgeDefinition edge, final JSONObject edgeDesignerConfig)
    {
        Validate.notNull(edge);
        Validate.notNull(edgeDesignerConfig);

        this.setId(edge.getId());
        this.setName(edge.getName());
        this.setDescription(edge.getDescription());
        this.setCheckExp(edge.getCheckExp());
        this.setRegExp(edge.getRegExp());
        this.setOnWalk(edge.getOnWalk());
        this.setTriggerOnEof(edge.getTriggerOnEof());
        this.setTriggerAlways(edge.getTriggerAlways());
        this.setRequiredConditions(edge.getRequiredConditions());
        this.timeIntervalSinceLastMicrotransition.fromTimeInterval(edge.getTimeIntervalSinceLastMicrotransition());
        this.timeIntervalSinceLastTransition.fromTimeInterval(edge.getTimeIntervalSinceLastTransition());
        this.timeIntervalSinceAutomatonStart.fromTimeInterval(edge.getTimeIntervalSinceAutomatonStart());
        this.timeIntervalForEvent.fromTimeInterval(edge.getTimeIntervalForEvent());
        this.timingConditionsUpdated();
        this.setChannel(edge.getChannel());

        PropertyNode.loadAllPropertyNodesFromJson(this.getPropertyNodes(), edgeDesignerConfig);

        final Double posX = edgeDesignerConfig.optDouble(JSON_KEY_EDGE_POS_X);
        final Double posY = edgeDesignerConfig.optDouble(JSON_KEY_EDGE_POS_Y);
        if (!Double.isNaN(posX) && !Double.isNaN(posY))
        {
            this.setPosition(new Point2D(posX, posY));
        }
    }

    void saveToJson(final JSONObject edgeDesignerConfig)
    {
        Validate.notNull(edgeDesignerConfig);

        PropertyNode.saveAllPropertyNodesToJson(this.getPropertyNodes(), edgeDesignerConfig);

        edgeDesignerConfig.put(JSON_KEY_EDGE_POS_X, this.getPosition().getX());
        edgeDesignerConfig.put(JSON_KEY_EDGE_POS_Y, this.getPosition().getY());
    }
}
