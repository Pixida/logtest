/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

class CircularNode extends BaseNode
{
    public static final Color DEFAULT_COLOR = Color.BLACK;

    private final Circle circle = new Circle();
    private Color color;

    CircularNode(final Graph graph, final int zIndex)
    {
        super(graph);

        final double defaultRadius = 5.0;
        this.circle.setRadius(defaultRadius);
        this.circle.setStroke(Color.BLACK);
        final double defaultStrokeWidth = 2.0;
        this.circle.setStrokeWidth(defaultStrokeWidth);
        this.setColor(DEFAULT_COLOR);

        this.registerPart(this.circle, zIndex);
        this.registerActionHandler(this.circle);
    }

    @Override
    public Point2D getConcreteDockingPointForConnection(final Point2D otherEnd)
    {
        // TODO: Return intersection between line and circle
        return this.getPosition();
    }

    @Override
    public Point2D getDockingPointForConnection()
    {
        return this.getPosition();
    }

    @Override
    protected Point2D getPosition()
    {
        return new Point2D(this.circle.getCenterX(), this.circle.getCenterY());
    }

    @Override
    protected void setPosition(final Point2D value)
    {
        this.circle.setCenterX(Math.max(this.circle.getRadius(), value.getX()));
        this.circle.setCenterY(Math.max(this.circle.getRadius(), value.getY()));
    }

    void setColor(final Color value)
    {
        this.color = value;
        this.circle.setFill(this.color);
    }
}
