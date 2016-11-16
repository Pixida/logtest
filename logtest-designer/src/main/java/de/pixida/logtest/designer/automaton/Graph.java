/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import de.pixida.logtest.designer.commons.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

class Graph
{
    private final Pane pane = new Pane();
    private final Map<BaseObject, List<GraphShape>> objects = new HashMap<>();
    private final List<GraphShape> shapes = new ArrayList<>();

    private final IAutomatonEditor automatonEditor;

    private BaseNode connectorSourceNode;
    private BaseEdge connector;
    private BaseNode currentConnectorTargetNode;

    private Point2D dragPressedPoint;
    private Point2D dragOriginalObjectPosition;

    private final ContextMenu contextMenu = new ContextMenu();

    private double lastMousePositionOnPaneX = 0d;
    private double lastMousePositionOnPaneY = 0d;

    Graph(final IAutomatonEditor aAutomatonEditor)
    {
        Validate.notNull(aAutomatonEditor);
        this.automatonEditor = aAutomatonEditor;

        this.pane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        final double minSizeX = 800.0;
        final double minSizeY = 600.0;
        this.pane.setMinSize(minSizeX, minSizeY);
        final double paddingRightBottom = 8.0;
        this.pane.setPadding(new Insets(0.0, paddingRightBottom, paddingRightBottom, 0.0));
        this.pane.setOnMouseMoved(event -> this.mouseMovedOnPane(event));
        this.pane.setOnMouseClicked(event -> this.mouseClickedOnPane(event));
        final MenuItem mi = new MenuItem("Create new state");
        mi.setGraphic(Icons.getIconGraphics("pencil_add"));
        mi.setOnAction(event -> {
            // The following code does not really belong here as the class is designed only to handle business belongings... it should be
            // moved somewhere else!?
            final AutomatonNode newNode = new AutomatonNode(Graph.this);
            newNode.setPosition(new Point2D(this.lastMousePositionOnPaneX, this.lastMousePositionOnPaneY));
            newNode.setName("New state");
            Graph.this.addObject(newNode);
            Graph.this.handleChange();
            Graph.this.showConfigFrameOfObject(newNode);
        });
        this.contextMenu.getItems().add(mi);
    }

    Pane getPane()
    {
        return this.pane;
    }

    void addObject(final BaseObject value)
    {
        Validate.notNull(value);
        Validate.isTrue(!this.objects.containsKey(value));
        final List<GraphShape> newShapes = value.getParts();
        Validate.notNull(newShapes);
        this.objects.put(value, newShapes);
        this.shapes.addAll(newShapes);
        Collections.sort(this.shapes, (shape1, shape2) -> Integer.compare(shape1.getZIndex(), shape2.getZIndex()));
        this.pane.getChildren().clear();
        this.pane.getChildren().addAll(this.shapes.stream().map(shape -> shape.getNode()).collect(Collectors.toList()));
    }

    void removeAllObjects()
    {
        this.pane.getChildren().clear();
        this.objects.clear();
        this.shapes.clear();
    }

    void removeObject(final BaseObject value)
    {
        Validate.notNull(value);

        // Would have liked to write: Validate.isTrue(this.objects.containsKey(value));
        // Nonetheless, this does not work well for forward and backward dependencies between nodes, where a node might be deleted
        // multiple times. This would be very confusing to solve, so it's easier to just allow deleting a node multiple times.
        // Consider: A -[target is dependent]-> B -[source is dependent]-> C
        // I.e. B must be deleted when A is deleted and B must be deleted when C is deleted.
        // In this case, when deleting A, A detaches AB, so that B is deleted (dependency), so that BC is deleted, so that B is deleted
        // (dependency) again.
        if (!this.objects.containsKey(value))
        {
            return;
        }

        final List<GraphShape> shapesToRemove = this.objects.get(value);
        this.shapes.removeAll(shapesToRemove);
        this.pane.getChildren().removeAll(shapesToRemove.stream().map(shape -> shape.getNode()).collect(Collectors.toList()));
        this.objects.remove(value);
    }

    public void onMouseEntered(final BaseObject baseObject, final MouseEvent event)
    {
        if (this.connectorSourceNode != null)
        {
            Validate.notNull(this.connector);
            if (baseObject instanceof BaseNode)
            {
                final BaseNode baseNode = (BaseNode) baseObject;
                if (this.connector.isAttachable(this.connectorSourceNode, baseNode))
                {
                    this.currentConnectorTargetNode = baseNode;
                    this.connector.align(this.connectorSourceNode, this.currentConnectorTargetNode);
                }
            }
        }
    }

    public void onMouseExited(final BaseObject baseObject, final MouseEvent event)
    {
        this.currentConnectorTargetNode = null;
    }

    public void onMousePressed(final BaseObject baseObject, final MouseEvent event)
    {
        if (this.showConfigFrameOfObject(baseObject))
        {
            event.consume();
        }
        if (baseObject instanceof BaseNode)
        {
            final BaseNode baseNode = (BaseNode) baseObject;
            this.dragPressedPoint = new Point2D(event.getX(), event.getY());
            this.dragOriginalObjectPosition = baseNode.getPosition();
        }
    }

    public void onMouseDragged(final BaseObject baseObject, final MouseEvent event)
    {
        if (baseObject instanceof BaseNode)
        {
            final BaseNode baseNode = (BaseNode) baseObject;
            Validate.notNull(this.dragPressedPoint);
            Validate.notNull(this.dragOriginalObjectPosition);

            final Point2D mouseMovement = new Point2D(event.getX(), event.getY()).subtract(this.dragPressedPoint);
            if (mouseMovement.getX() != 0d || mouseMovement.getY() != 0d)
            {
                Point2D newRectPos = this.dragOriginalObjectPosition.add(mouseMovement);
                newRectPos = new Point2D(Math.max(0d, newRectPos.getX()), Math.max(0d, newRectPos.getY()));
                baseNode.moveTo(newRectPos);
                this.handleMinorChange();
            }
        }
    }

    public void onMouseClicked(final BaseObject baseObject, final MouseEvent event)
    {
        Validate.notNull(baseObject);

        this.contextMenu.hide();

        if (event.getButton() == MouseButton.PRIMARY)
        {
            if (this.connectorSourceNode != null)
            {
                Validate.notNull(this.connector);
                if (baseObject == this.currentConnectorTargetNode)
                {
                    this.connector.attach(this.connectorSourceNode, this.currentConnectorTargetNode);

                    this.connector = null;
                    this.connectorSourceNode = null;
                    this.currentConnectorTargetNode = null;

                    this.handleChange();
                }
            }
            event.consume();
        }
        else if (event.getButton() == MouseButton.SECONDARY)
        {
            final ContextMenu objectContextMenu = baseObject.createContextMenu();
            if (objectContextMenu != null)
            {
                Node contextMenuOwner = baseObject.getActionHandler();
                if (contextMenuOwner == null)
                {
                    contextMenuOwner = this.pane;
                }
                objectContextMenu.show(contextMenuOwner, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        }
    }

    public boolean showConfigFrameOfObject(final BaseObject baseObject)
    {
        Validate.notNull(baseObject);
        final Node configFrame = baseObject.getConfigFrame();
        if (configFrame != null)
        {
            this.automatonEditor.showConfigFrame(configFrame);
            return true;
        }
        else
        {
            this.automatonEditor.showDefaultConfigFrame();
            return false;
        }
    }

    private void mouseMovedOnPane(final MouseEvent event)
    {
        // Using double properties here to avoid spamming the heap with immutable objects like Point2D
        this.lastMousePositionOnPaneX = event.getX();
        this.lastMousePositionOnPaneY = event.getY();

        if (this.connectorSourceNode != null)
        {
            Validate.notNull(this.connector);
            if (this.currentConnectorTargetNode == null)
            {
                // Otherwise, connector was already aligned when object was hovered
                this.connector.align(this.connectorSourceNode, new Point2D(event.getX(), event.getY()));
            }
        }
    }

    private void mouseClickedOnPane(final MouseEvent event)
    {
        this.contextMenu.hide();
        if (event.getButton() == MouseButton.PRIMARY)
        {
            if (this.connectorSourceNode != null)
            {
                Validate.notNull(this.connector);
                this.cancelDrawingConnector();
            }
            else
            {
                this.automatonEditor.showDefaultConfigFrame();
            }
        }
        else if (event.getButton() == MouseButton.SECONDARY)
        {
            this.contextMenu.show(this.pane, event.getScreenX(), event.getScreenY());
        }
    }

    void startDrawingConnector(final BaseNode aConnectorSourceObject, final BaseEdge aConnector)
    {
        Validate.notNull(aConnectorSourceObject);
        Validate.notNull(aConnector);
        this.connectorSourceNode = aConnectorSourceObject;
        this.connector = aConnector;
        this.addObject(aConnector);
        this.connector.align(this.connectorSourceNode, new Point2D(this.lastMousePositionOnPaneX, this.lastMousePositionOnPaneY));
    }

    private void cancelDrawingConnector()
    {
        if (this.connectorSourceNode != null)
        {
            Validate.notNull(this.connector);
            this.removeObject(this.connector);
            this.connector = null;
            this.connectorSourceNode = null;
            this.currentConnectorTargetNode = null;
        }
    }

    void handleChange()
    {
        this.automatonEditor.handleChange();
    }

    void handleMinorChange()
    {
        this.automatonEditor.handleMinorChange();
    }

    @SuppressWarnings("unchecked")
    <T> List<T> getAllNodesByClass(final Class<T> clz)
    {
        return this.objects.keySet().stream().filter(node -> clz.isAssignableFrom(node.getClass())).map(node -> (T) node)
            .collect(Collectors.toList());
    }
}
