/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;

/** I wonder why I have to implement it by myself - isn't there a library for that!? */
abstract class LineIntersector
{
    private static final double EPSILON = 10E-5;
    private static final Rotate ROATE_90_DEGREES_COUNTERCLOCKWISE = new Rotate(-90.0);

    static class Intersection
    {
        private final Point2D intersection;
        private final Line line;
        private final double distance;

        Intersection(final Point2D aIntersection, final Line aLine, final double aDistance)
        {
            Validate.notNull(aIntersection);
            Validate.notNull(aLine);
            Validate.isTrue(aDistance >= 0d);
            this.intersection = aIntersection;
            this.line = aLine;
            this.distance = aDistance;
        }

        Point2D getIntersection()
        {
            return this.intersection;
        }

        Line getLine()
        {
            return this.line;
        }

        double getDistance()
        {
            return this.distance;
        }
    }

    enum PointPosition
    {
        LEFT_OF_THE_LINE,
        RIGHT_OF_THE_LINE,
        ON_SAME_LINE
    }

    static List<Intersection> calculateIntersections(final Point2D point, final Point2D vec, final List<Line> lines)
    {
        Validate.notNull(point);
        Validate.notNull(vec);
        Validate.notNull(lines);

        final List<Intersection> results = new ArrayList<>();
        for (final Line line : lines)
        {
            final Point2D intersection = calculateIntersection(point, vec, line);
            if (intersection == null)
            {
                continue;
            }

            if (!checkIfPointIsOnTheLine(intersection, line))
            {
                continue;
            }

            results.add(new Intersection(intersection, line, point.distance(intersection)));
        }

        Collections.sort(results, (r0, r1) -> Double.compare(r0.getDistance(), r1.getDistance()));

        return results;
    }

    static boolean checkIfPointIsOnTheLine(final Point2D point, final Line line)
    {
        final Point2D lineStart = new Point2D(line.getStartX(), line.getStartY());
        final Point2D lineEnd = new Point2D(line.getEndX(), line.getEndY());
        final Point2D vecEndToStart = lineEnd.subtract(lineStart);
        final Point2D vecStartToEnd = lineStart.subtract(lineEnd);
        final Point2D vecEndToStartRotated = ROATE_90_DEGREES_COUNTERCLOCKWISE.transform(vecEndToStart);
        final Point2D vecStartToEndRotated = ROATE_90_DEGREES_COUNTERCLOCKWISE.transform(vecStartToEnd);
        if (getPointSideOfLine(point, lineEnd, lineEnd.add(vecEndToStartRotated)) == PointPosition.LEFT_OF_THE_LINE)
        {
            return false;
        }
        if (getPointSideOfLine(point, lineStart, lineStart.add(vecStartToEndRotated)) == PointPosition.LEFT_OF_THE_LINE)
        {
            return false;
        }
        return true;
    }

    // Reference: http://stackoverflow.com/questions/22668659/calculate-on-which-side-of-a-line-a-point-is
    static PointPosition getPointSideOfLine(final Point2D point, final Point2D lineStart, final Point2D lineEnd)
    {
        final double value = (lineEnd.getX() - lineStart.getX()) * (point.getY() - lineStart.getY())
            - (point.getX() - lineStart.getX()) * (lineEnd.getY() - point.getY());
        if (Math.abs(value) < EPSILON)
        {
            return PointPosition.ON_SAME_LINE;
        }
        if (value < 0.0)
        {
            return PointPosition.RIGHT_OF_THE_LINE;
        }
        return PointPosition.LEFT_OF_THE_LINE;
    }

    static Point2D calculateIntersection(final Point2D point, final Point2D vec, final Line line)
    {
        final Point2D q = new Point2D(line.getStartX(), line.getStartY());
        final Point2D w = new Point2D(line.getEndX(), line.getEndY()).subtract(q);

        // q + t1 * w = point + t2 * vec => q - point = (vec w) (t2 -t1)^T
        final double det = det2x2LineWiseDefined(vec.getX(), w.getX(), vec.getY(), w.getY());
        final double epsilon = 1E-5;
        if (Math.abs(det) < epsilon)
        {
            return null;
        }
        final double detInv = 1.0 / det;
        final double t2 = det2x2LineWiseDefined(q.getX() - point.getX(), w.getX(), q.getY() - point.getY(), w.getY()) * detInv;

        final Point2D intersection = vec.multiply(t2).add(point);
        return intersection;
    }

    private static double det2x2LineWiseDefined(final double a, final double b, final double c, final double d)
    {
        return a * d - b * c;
    }
}
