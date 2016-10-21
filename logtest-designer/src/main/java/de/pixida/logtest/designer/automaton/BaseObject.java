/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.Validate;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;

class BaseObject
{
    private final Graph graph;

    private final LinkedHashMap<Node, Integer> parts = new LinkedHashMap<>();
    private Node actionHandler;

    private boolean visible = true;

    BaseObject(final Graph aGraph)
    {
        Validate.notNull(aGraph);
        this.graph = aGraph;
    }

    boolean isVisible()
    {
        return this.visible;
    }

    void show()
    {
        this.visible = true;
        this.parts.keySet().forEach(part -> part.setVisible(true));
    }

    void hide()
    {
        this.visible = false;
        this.parts.keySet().forEach(part -> part.setVisible(false));
    }

    protected void registerPart(final Node node, final int zIndex)
    {
        Validate.notNull(node);
        Validate.isTrue(!this.parts.containsKey(node));
        this.parts.put(node, zIndex);
    }

    protected void removePart(final Node node)
    {
        Validate.notNull(node);
        Validate.isTrue(this.parts.containsKey(node));
        this.parts.remove(node);
        if (node == this.actionHandler)
        {
            this.unregisterActionHandler();
        }
    }

    protected Graph getGraph()
    {
        return this.graph;
    }

    List<GraphShape> getParts()
    {
        final List<GraphShape> results = new ArrayList<>(this.parts.size());
        this.parts.forEach((node, zIndex) -> results.add(new GraphShape(node, zIndex)));
        return results;
    }

    protected void registerActionHandler(final Node node)
    {
        Validate.notNull(node);
        Validate.isTrue(this.parts.containsKey(node));

        this.unregisterActionHandler();

        this.actionHandler = node;
        this.actionHandler.setOnMouseEntered(event -> this.graph.onMouseEntered(this, event));
        this.actionHandler.setOnMouseExited(event -> this.graph.onMouseExited(this, event));
        this.actionHandler.setOnMousePressed(event -> this.graph.onMousePressed(this, event));
        this.actionHandler.setOnMouseDragged(event -> this.graph.onMouseDragged(this, event));
        this.actionHandler.setOnMouseClicked(event -> this.graph.onMouseClicked(this, event));
    }

    private void unregisterActionHandler()
    {
        if (this.actionHandler != null)
        {
            final EventHandler<? super MouseEvent> voidHandler = event -> {
                // Do nothing
            };
            this.actionHandler.setOnMouseEntered(voidHandler);
            this.actionHandler.setOnMouseExited(voidHandler);
            this.actionHandler.setOnMousePressed(voidHandler);
            this.actionHandler.setOnMouseDragged(voidHandler);
            this.actionHandler.setOnMouseClicked(voidHandler);

            this.actionHandler = null;
        }
    }

    ContextMenu createContextMenu()
    {
        return null;
    }

    Node getActionHandler()
    {
        return this.actionHandler;
    }

    Node getConfigFrame()
    {
        return null;
    }
}
