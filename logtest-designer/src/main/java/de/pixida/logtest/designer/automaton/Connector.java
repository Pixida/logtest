/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;

class Connector extends BaseEdge
{
    private final Line line = new Line();
    private final Polygon arrowhead = new Polygon();

    private Point2D connectorStart;
    private Point2D connectorEnd;

    private boolean showArrow = true;

    Connector(final Graph graph, final int zIndex)
    {
        super(graph);

        // Default line settings
        final double defaultStrokeWidth = 2.0;
        this.line.setStrokeWidth(defaultStrokeWidth);

        this.registerPart(this.line, zIndex);
        this.registerPart(this.arrowhead, zIndex);
    }

    void hideArrow()
    {
        this.showArrow = false;
    }

    void setDashed()
    {
        final double lineLength = 3d;
        final double pauseLength = 6d;
        this.line.getStrokeDashArray().addAll(lineLength, pauseLength);
    }

    @Override
    void setStart(final Point2D value)
    {
        this.line.setStartX(value.getX());
        this.line.setStartY(value.getY());
        this.connectorStart = Point2D.ZERO.add(value);
        this.alignArrow();
    }

    @Override
    void setEnd(final Point2D value)
    {
        this.line.setEndX(value.getX());
        this.line.setEndY(value.getY());
        this.connectorEnd = Point2D.ZERO.add(value);
        this.alignArrow();
    }

    @Override
    boolean isAttachable(final BaseNode aSourceNode, final BaseNode aTargetNode)
    {
        // Edge is generic
        return true;
    }

    void setColor(final Color value)
    {
        this.line.setStroke(value);
        this.arrowhead.setFill(value);
    }

    protected Point2D getConnectorStart()
    {
        return this.connectorStart;
    }

    protected Point2D getConnectorEnd()
    {
        return this.connectorEnd;
    }

    private void alignArrow()
    {
        if (!this.showArrow)
        {
            return;
        }

        final Point2D start = new Point2D(this.line.getStartX(), this.line.getStartY());
        final Point2D end = new Point2D(this.line.getEndX(), this.line.getEndY());
        final Point2D vEndToStart = end.subtract(start).normalize();
        final double angle = 90.0;
        final Point2D vEndToStartOrt1 = new Rotate(angle).transform(Point2D.ZERO.add(vEndToStart)).normalize();
        final Point2D vEndToStartOrt2 = new Rotate(-angle).transform(Point2D.ZERO.add(vEndToStart)).normalize();
        final double innerLength = 8.0;
        final double outerLength = 12.5;
        final double widthOfOneSide = 5.0;
        this.arrowhead.getPoints().clear();

        Point2D next = end;
        this.addPointToArrowHead(next);
        next = end.subtract(vEndToStart.multiply(outerLength)).add(vEndToStartOrt2.multiply(widthOfOneSide));
        this.addPointToArrowHead(next);
        next = end.subtract(vEndToStart.multiply(innerLength));
        this.moveEndOfLineIntoArrowAsTheLineIsThickerThanTheArrowPike(next);
        this.addPointToArrowHead(next);
        next = end.subtract(vEndToStart.multiply(outerLength)).add(vEndToStartOrt1.multiply(widthOfOneSide));
        this.addPointToArrowHead(next);
    }

    private void moveEndOfLineIntoArrowAsTheLineIsThickerThanTheArrowPike(final Point2D next)
    {
        this.line.setEndX(next.getX());
        this.line.setEndY(next.getY());
    }

    private void addPointToArrowHead(final Point2D value)
    {
        this.arrowhead.getPoints().addAll(value.getX(), value.getY());
    }
}
