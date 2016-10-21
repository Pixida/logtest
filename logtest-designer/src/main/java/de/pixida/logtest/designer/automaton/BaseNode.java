/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import javafx.geometry.Point2D;

abstract class BaseNode extends BaseObject
{
    private final List<BaseEdge> incomingEdges = new ArrayList<>();
    private final List<BaseEdge> outgoingEdges = new ArrayList<>();

    BaseNode(final Graph graph)
    {
        super(graph);
    }

    void addOutgoingConnector(final BaseEdge edge, final BaseNode targetObject)
    {
        Validate.notNull(edge);
        Validate.notNull(targetObject);
        this.outgoingEdges.add(edge);
    }

    void addIncomingConnector(final BaseEdge edge, final BaseNode sourceObject)
    {
        Validate.notNull(edge);
        Validate.notNull(sourceObject);
        this.incomingEdges.add(edge);
    }

    void removeOutgoingConnector(final BaseEdge edge)
    {
        Validate.notNull(edge);
        final boolean removed = this.outgoingEdges.remove(edge);
        Validate.isTrue(removed);
    }

    void removeIncomingConnector(final BaseEdge edge)
    {
        Validate.notNull(edge);
        final boolean removed = this.incomingEdges.remove(edge);
        Validate.isTrue(removed);
    }

    void moveBy(final Point2D relativeMovement)
    {
        this.moveTo(this.getPosition().add(relativeMovement));
    }

    void moveTo(final Point2D targetPosition)
    {
        final Point2D oldPosition = Point2D.ZERO.add(this.getPosition()); // Deep copy
        this.setPosition(targetPosition);
        this.processMoveOperation(oldPosition, targetPosition);
    }

    protected void processMoveOperation(final Point2D oldPosition, final Point2D newPosition)
    {
        final Point2D relativeMovement = newPosition.subtract(oldPosition);
        this.incomingEdges.forEach(edge -> {
            edge.onTargetNodeMoved(relativeMovement);
        });
        this.outgoingEdges.forEach(edge -> {
            edge.onSourceNodeMoved(relativeMovement);
        });
        this.realignEdgesAfterMoveOrResize();
    }

    protected void realignEdgesAfterMoveOrResize()
    {
        this.incomingEdges.forEach(edge -> edge.realign());
        this.outgoingEdges.forEach(edge -> edge.realign());
    }

    void removeNodeAndEdges()
    {
        new ArrayList<>(this.incomingEdges).forEach(edge -> {
            edge.detach();
            this.getGraph().removeObject(edge);
        });
        new ArrayList<>(this.outgoingEdges).forEach(edge -> {
            edge.detach();
            this.getGraph().removeObject(edge);
        });
        this.getGraph().removeObject(this);
    }

    protected <T extends BaseNode> List<T> getPreviousNodesFilteredByType(final Class<T> clz)
    {
        return this.incomingEdges.stream().filter(edge -> edge.sourceNodeIsOfType(clz)).map(edge -> edge.getSourceNode(clz))
            .collect(Collectors.toList());
    }

    protected <T extends BaseNode> List<T> getNextNodesFilteredByType(final Class<T> clz)
    {
        return this.outgoingEdges.stream().filter(edge -> edge.targetNodeIsOfType(clz)).map(edge -> edge.getTargetNode(clz))
            .collect(Collectors.toList());
    }

    protected abstract Point2D getConcreteDockingPointForConnection(Point2D otherEnd);

    protected abstract Point2D getDockingPointForConnection();

    protected abstract Point2D getPosition();

    protected abstract void setPosition(Point2D position);
}
