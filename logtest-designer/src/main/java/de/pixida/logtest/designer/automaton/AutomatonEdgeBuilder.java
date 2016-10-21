/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import org.apache.commons.lang3.Validate;

import javafx.geometry.Point2D;

class AutomatonEdgeBuilder extends Connector
{
    private static final int Z_INDEX_AUTOMATON_EDGE_NODE_CONNECTORS = 140;

    private AutomatonEdge createdEdge;

    AutomatonEdgeBuilder(final Graph graph)
    {
        super(graph, Z_INDEX_AUTOMATON_EDGE_NODE_CONNECTORS);
    }

    @Override
    void attach(final BaseNode sourceObject, final BaseNode targetObject)
    {
        // Actually we do not attach the edge here. We now know the user wants to draw an automaton edge from source to target. As
        // automaton edges are nodes, we remove this edge and instead create an immediate node representing the automaton edge,
        // and two edges instead of this one to connect the new immediate node.
        super.attach(sourceObject, targetObject); // Calculates the start and end point which we will use to calculate the center

        this.align(sourceObject, targetObject);
        final Point2D center = this.getConnectorStart().add(this.getConnectorEnd()).multiply(0.5);
        final AutomatonEdge newEdge = new AutomatonEdge(this.getGraph());
        newEdge.setPosition(center);
        this.getGraph().addObject(newEdge);
        final Connector sourceObjectToNewNode = new Connector(this.getGraph(), Z_INDEX_AUTOMATON_EDGE_NODE_CONNECTORS);
        sourceObjectToNewNode.hideArrow();
        sourceObjectToNewNode.attach(sourceObject, newEdge);
        final Connector newNodeToTargetObject = new Connector(this.getGraph(), Z_INDEX_AUTOMATON_EDGE_NODE_CONNECTORS);
        newNodeToTargetObject.attach(newEdge, targetObject);
        sourceObjectToNewNode.realign();
        newNodeToTargetObject.realign();
        sourceObjectToNewNode.targetIsDependentNode();
        newNodeToTargetObject.sourceIsDependentNode();
        this.getGraph().addObject(sourceObjectToNewNode);
        this.getGraph().addObject(newNodeToTargetObject);

        this.detach();
        this.getGraph().removeObject(this);

        this.createdEdge = newEdge;
    }

    AutomatonEdge getCreatedEdge()
    {
        Validate.notNull(this.createdEdge);
        return this.createdEdge;
    }

    @Override
    boolean isAttachable(final BaseNode aSourceNode, final BaseNode aTargetNode)
    {
        return aSourceNode instanceof AutomatonNode && aTargetNode instanceof AutomatonNode;
    }
}
