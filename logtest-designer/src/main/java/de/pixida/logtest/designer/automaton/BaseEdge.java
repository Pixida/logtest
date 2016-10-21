/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import org.apache.commons.lang3.Validate;

import javafx.geometry.Point2D;

abstract class BaseEdge extends BaseObject
{
    private BaseNode sourceNode;
    private BaseNode targetNode;

    private boolean targetIsDependent = false;
    private boolean sourceIsDependent = false;

    BaseEdge(final Graph graph)
    {
        super(graph);
    }

    void align(final BaseNode aSourceNode, final BaseNode aTargetNode)
    {
        Validate.notNull(aSourceNode);
        Validate.notNull(aTargetNode);
        this.align(aSourceNode, aTargetNode.getConcreteDockingPointForConnection(aSourceNode.getDockingPointForConnection()));
    }

    void align(final BaseNode aSourceNode, final Point2D aTargetPosition)
    {
        Validate.notNull(aSourceNode);
        Validate.notNull(aTargetPosition);
        this.setStart(aSourceNode.getConcreteDockingPointForConnection(aTargetPosition));
        this.setEnd(aTargetPosition);
    }

    void attach(final BaseNode aSourceNode, final BaseNode aTargetNode)
    {
        Validate.notNull(aSourceNode);
        Validate.notNull(aTargetNode);
        Validate.isTrue(this.isAttachable(aSourceNode, aTargetNode));
        this.detach();
        this.sourceNode = aSourceNode;
        this.targetNode = aTargetNode;
        this.sourceNode.addOutgoingConnector(this, this.targetNode);
        this.targetNode.addIncomingConnector(this, this.sourceNode);
    }

    void realign()
    {
        Validate.notNull(this.sourceNode);
        Validate.notNull(this.targetNode);
        this.align(this.sourceNode, this.targetNode);
    }

    void targetIsDependentNode()
    {
        this.targetIsDependent = true;
    }

    void sourceIsDependentNode()
    {
        this.sourceIsDependent = true;
    }

    boolean sourceNodeIsOfType(final Class<?> clz)
    {
        Validate.notNull(clz);
        Validate.notNull(this.sourceNode);
        return clz.isAssignableFrom(this.sourceNode.getClass());
    }

    boolean targetNodeIsOfType(final Class<?> clz)
    {
        Validate.notNull(clz);
        Validate.notNull(this.targetNode);
        return clz.isAssignableFrom(this.targetNode.getClass());
    }

    <T> T getSourceNode(final Class<T> clz)
    {
        Validate.notNull(clz);
        Validate.isTrue(this.sourceNodeIsOfType(clz));
        return clz.cast(this.sourceNode);
    }

    <T> T getTargetNode(final Class<T> clz)
    {
        Validate.notNull(clz);
        Validate.isTrue(this.targetNodeIsOfType(clz));
        return clz.cast(this.targetNode);
    }

    void detach()
    {
        if (this.sourceNode != null)
        {
            Validate.notNull(this.targetNode);

            this.sourceNode.removeOutgoingConnector(this);
            if (this.sourceIsDependent)
            {
                this.sourceNode.removeNodeAndEdges();
            }
            this.sourceNode = null;

            this.targetNode.removeIncomingConnector(this);
            if (this.targetIsDependent)
            {
                this.targetNode.removeNodeAndEdges();
            }
            this.targetNode = null;
        }
    }

    void onSourceNodeMoved(final Point2D relativeMovement)
    {
        Validate.notNull(this.sourceNode);
        Validate.notNull(this.targetNode);
        if (this.targetIsDependent)
        {
            if (!this.targetNode.getNextNodesFilteredByType(this.sourceNode.getClass()).contains(this.sourceNode))
            {
                this.targetNode.moveBy(relativeMovement);
                this.targetNode.realignEdgesAfterMoveOrResize();
            }
        }
    }

    void onTargetNodeMoved(final Point2D relativeMovement)
    {
        Validate.notNull(this.sourceNode);
        if (this.sourceIsDependent)
        {
            this.sourceNode.moveBy(relativeMovement);
            this.sourceNode.realignEdgesAfterMoveOrResize();
        }
    }

    abstract void setStart(Point2D value);

    abstract void setEnd(Point2D value);

    abstract boolean isAttachable(final BaseNode aSourceNode, final BaseNode aTargetNode);
}
