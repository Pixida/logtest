/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONObject;

import de.pixida.logtest.designer.automaton.LineIntersector.Intersection;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

class RectangularNode extends BaseNode
{
    private static final Color BORDER_COLOR_DEFAULT = Color.BLACK;
    //    private static final Color BORDER_COLOR_SELECTED = Color.RED;
    private static final double DEFAULT_H = 50.0;
    private static final double DEFAULT_W = 90.0;
    private static final double DEFAULT_Y = 10.0;
    private static final double DEFAULT_X = 10.0;

    public enum ContentDisplayMode
    {
        OVERFLOW(true, (theTextFlow, theRectangle) -> {
            // Nothing to do
        }),
        CLIP(true, (theTextFlow, theRectangle) -> {
            final Rectangle clipRect = new Rectangle();
            clipRect.widthProperty().bind(theRectangle.widthProperty());
            clipRect.heightProperty().bind(theRectangle.heightProperty());
            theTextFlow.setClip(clipRect);
        }),
        ADJUST_RECT(false, (theTextFlow, theRectangle) -> {
            theRectangle.widthProperty().bind(theTextFlow.widthProperty());
            theRectangle.heightProperty().bind(theTextFlow.heightProperty());
        });

        private BiConsumer<TextFlow, Rectangle> initializer;
        private boolean resizable;

        private ContentDisplayMode(final boolean aResizable, final BiConsumer<TextFlow, Rectangle> aInitializer)
        {
            this.resizable = aResizable;
            this.initializer = aInitializer;
        }

        private void init(final TextFlow theTextFlow, final Rectangle theRectangle)
        {
            this.initializer.accept(theTextFlow, theRectangle);
        }

        private boolean isResizable()
        {
            return this.resizable;
        }
    }

    private static final String JSON_KEY_H = "h";
    private static final String JSON_KEY_W = "w";
    private static final String JSON_KEY_Y = "y";
    private static final String JSON_KEY_X = "x";

    private final ContentDisplayMode contentDisplayMode;

    private final Rectangle rectangle = new Rectangle();
    private final Text titleText = new Text();
    private final Text separatorText = new Text("\n");
    private final Text contentText = new Text();
    private final TextFlow textFlow = new TextFlow();

    private String title;
    private String content;
    private Color color;

    //    private Point2D originalClickedPoint;
    //    private Point2D originalRectPoint;

    //    private final Rectangle resizeSpotTopLeft = new Rectangle();
    //    private final Rectangle resizeSpotTopRight = new Rectangle();
    //    private final Rectangle resizeSpotBottomLeft = new Rectangle();
    //    private final Rectangle resizeSpotBottomRight = new Rectangle();
    //    private final Rectangle[] resizeSpots = {
    //                    this.resizeSpotTopLeft,
    //                    this.resizeSpotTopRight,
    //                    this.resizeSpotBottomLeft,
    //                    this.resizeSpotBottomRight
    //    };
    //    private Rectangle originalPosition;

    RectangularNode(final Graph graph, final ContentDisplayMode aContentDisplayMode, final int zIndex)
    {
        super(graph);

        Validate.notNull(aContentDisplayMode);
        this.contentDisplayMode = aContentDisplayMode;

        this.rectangle.setStroke(BORDER_COLOR_DEFAULT);

        final Font titleFont = Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 10.0);
        this.titleText.setFont(titleFont);

        final Font contentFont = Font.font(titleFont.getFamily(), FontWeight.NORMAL, titleFont.getSize());
        this.contentText.setFont(contentFont);

        this.textFlow.getChildren().addAll(this.titleText, this.separatorText, this.contentText);
        this.textFlow.layoutXProperty().bind(this.rectangle.xProperty());
        this.textFlow.layoutYProperty().bind(this.rectangle.yProperty());
        final double insetTop = 0.0;
        final double insetRight = 3.0;
        final double insetBottom = 3.0;
        final double insetLeft = 3.0;
        this.textFlow.setPadding(new Insets(insetTop, insetRight, insetBottom, insetLeft));
        this.textFlow.setMouseTransparent(true);

        this.contentDisplayMode.init(this.textFlow, this.rectangle);

        if (this.contentDisplayMode.isResizable())
        {
            //            this.createResizeSpots();
        }

        this.registerPart(this.rectangle, zIndex);
        this.registerPart(this.textFlow, zIndex);
        this.registerActionHandler(this.rectangle);

        this.loadDimensionsFromJson(new JSONObject());

        this.setColor(Color.WHITE);

        this.setContent(null);
    }

    //    private void createResizeSpots()
    //    {
    //        final double rectOverlap = 4.0;
    //        this.resizeSpotTopLeft.layoutXProperty().bind(this.rectangle.xProperty().subtract(rectOverlap));
    //        this.resizeSpotTopLeft.layoutYProperty().bind(this.rectangle.yProperty().subtract(rectOverlap));
    //        this.resizeSpotTopRight.layoutXProperty()
    //            .bind(this.rectangle.xProperty().add(this.rectangle.widthProperty()).subtract(rectOverlap));
    //        this.resizeSpotTopRight.layoutYProperty().bind(this.rectangle.yProperty().subtract(rectOverlap));
    //        this.resizeSpotBottomLeft.layoutXProperty().bind(this.rectangle.xProperty().subtract(rectOverlap));
    //        this.resizeSpotBottomLeft.layoutYProperty().bind(
    //            this.rectangle.yProperty().add(this.rectangle.heightProperty()).subtract(rectOverlap));
    //        this.resizeSpotBottomRight.layoutXProperty().bind(
    //            this.rectangle.xProperty().add(this.rectangle.widthProperty()).subtract(rectOverlap));
    //        this.resizeSpotBottomRight.layoutYProperty().bind(
    //            this.rectangle.yProperty().add(this.rectangle.heightProperty()).subtract(rectOverlap));
    //        Arrays.stream(this.resizeSpots).forEach(dot -> {
    //            dot.setWidth(rectOverlap * 2.0);
    //            dot.setHeight(rectOverlap * 2.0);
    //        });
    //        Arrays.stream(this.resizeSpots).forEach(dot -> {
    //            dot.setFill(Color.RED);
    //        });
    //        Arrays.stream(this.resizeSpots).forEach(dot -> {
    //            dot.setOnMousePressed(event -> {
    //                this.originalPosition = new Rectangle();
    //                this.originalPosition.setX(this.rectangle.getX());
    //                this.originalPosition.setY(this.rectangle.getY());
    //                this.originalPosition.setWidth(this.rectangle.getWidth());
    //                this.originalPosition.setHeight(this.rectangle.getHeight());
    //                this.originalClickedPoint = new Point2D(event.getSceneX(), event.getSceneY());
    //            });
    //        });
    //        Arrays.stream(this.resizeSpots).forEach(dot -> {
    //            dot.setOnMouseDragged(event -> {
    //                final Point2D mouseMovement = new Point2D(event.getSceneX(), event.getSceneY()).subtract(this.originalClickedPoint);
    //                if (mouseMovement.getX() != 0.0 || mouseMovement.getY() != 0.0)
    //                {
    //                    this.onMoveOrResize();
    //
    //                    if (dot == this.resizeSpotTopLeft)
    //                    {
    //                        this.handleLeftResize(mouseMovement);
    //                        this.handleTopResize(mouseMovement);
    //                    }
    //                    else if (dot == this.resizeSpotBottomLeft)
    //                    {
    //                        this.handleLeftResize(mouseMovement);
    //                        this.handleBottomResize(mouseMovement);
    //                    }
    //                    else if (dot == this.resizeSpotTopRight)
    //                    {
    //                        this.handleRightResize(mouseMovement);
    //                        this.handleTopResize(mouseMovement);
    //                    }
    //                    else if (dot == this.resizeSpotBottomRight)
    //                    {
    //                        this.handleRightResize(mouseMovement);
    //                        this.handleBottomResize(mouseMovement);
    //                    }
    //                    else
    //                    {
    //                        // This should never happen
    //                    }
    //                }
    //            });
    //        });
    //    }

    String getTitle()
    {
        return this.title;
    }

    void setTitle(final String value)
    {
        this.title = value;
        this.titleText.setText(value);
        this.showTitleAndContentOnlyIfNotBlank();
        this.immediatelyApplyChangedSizeOfRectangle();
    }

    String getContent()
    {
        return this.content;
    }

    void setContent(final String value)
    {
        this.content = value;
        this.contentText.setText(value);
        this.showTitleAndContentOnlyIfNotBlank();
        this.immediatelyApplyChangedSizeOfRectangle();
    }

    private void showTitleAndContentOnlyIfNotBlank()
    {
        final boolean gotTitle = StringUtils.isNotBlank(this.title);
        final boolean gotContent = StringUtils.isNotBlank(this.content);

        this.textFlow.getChildren().clear();

        if (gotTitle)
        {
            this.textFlow.getChildren().add(this.titleText);
        }
        if (gotTitle && gotContent)
        {
            this.textFlow.getChildren().add(this.separatorText);
        }
        if (gotContent)
        {
            this.textFlow.getChildren().add(this.contentText);
        }
    }

    Color getColor()
    {
        return this.color;
    }

    void setColor(final Color value)
    {
        this.color = value;
        this.rectangle.setFill(value);
    }

    void loadDimensionsFromJson(final JSONObject jsonObject)
    {
        Validate.notNull(jsonObject);

        // Set default position and size here
        this.rectangle.setX(jsonObject.optDouble(JSON_KEY_X, DEFAULT_X));
        this.rectangle.setY(jsonObject.optDouble(JSON_KEY_Y, DEFAULT_Y));

        // Cannot set width and height when values are bound. Only set when the mode is CLIP or OVERFLOW
        if (this.contentDisplayMode.isResizable())
        {
            this.rectangle.setWidth(jsonObject.optDouble(JSON_KEY_W, DEFAULT_W));
            this.rectangle.setHeight(jsonObject.optDouble(JSON_KEY_H, DEFAULT_H));
        }
    }

    void saveDimensionsToJson(final JSONObject rectConfig)
    {
        Validate.notNull(rectConfig);
        rectConfig.put(JSON_KEY_X, this.rectangle.getX());
        rectConfig.put(JSON_KEY_Y, this.rectangle.getY());
        rectConfig.put(JSON_KEY_W, this.rectangle.getWidth());
        rectConfig.put(JSON_KEY_H, this.rectangle.getHeight());
    }

    private Point2D getCenterPoint()
    {
        final double halfWay = 0.5;
        return new Point2D(this.rectangle.getX() + this.rectangle.getWidth() * halfWay,
            this.rectangle.getY() + this.rectangle.getHeight() * halfWay);
    }

    private List<Line> getBoundingLines()
    {
        final double l = this.rectangle.getX();
        final double t = this.rectangle.getY();
        final double b = t + this.rectangle.getHeight();
        final double r = l + this.rectangle.getWidth();
        final Point2D lt = new Point2D(l, t);
        final Point2D rt = new Point2D(r, t);
        final Point2D rb = new Point2D(r, b);
        final Point2D lb = new Point2D(l, b);
        return Arrays.asList(this.createLineBetweenTwoPoints(lt, rt), this.createLineBetweenTwoPoints(rt, rb),
            this.createLineBetweenTwoPoints(rb, lb), this.createLineBetweenTwoPoints(lb, lt));
    }

    private Line createLineBetweenTwoPoints(final Point2D from, final Point2D to)
    {
        return new Line(from.getX(), from.getY(), to.getX(), to.getY());
    }

    private void immediatelyApplyChangedSizeOfRectangle()
    {
        this.textFlow.autosize();
        this.realignEdgesAfterMoveOrResize();
    }

    @Override
    protected Point2D getConcreteDockingPointForConnection(final Point2D otherEnd)
    {
        final Point2D sourceToCenter = otherEnd.subtract(this.getCenterPoint());
        final List<Intersection> intersections = LineIntersector.calculateIntersections(otherEnd, sourceToCenter, this.getBoundingLines());
        if (intersections.isEmpty())
        {
            return this.getCenterPoint();
        }
        return intersections.get(0).getIntersection();
    }

    @Override
    protected Point2D getDockingPointForConnection()
    {
        return this.getCenterPoint();
    }

    @Override
    protected Point2D getPosition()
    {
        return new Point2D(this.rectangle.getX(), this.rectangle.getY());
    }

    @Override
    protected void setPosition(final Point2D position)
    {
        this.rectangle.setX(Math.max(0d, position.getX()));
        this.rectangle.setY(Math.max(0d, position.getY()));
    }
}
