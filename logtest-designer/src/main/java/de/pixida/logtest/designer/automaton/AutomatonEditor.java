/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.io.File;

import org.apache.commons.lang3.Validate;

import de.pixida.logtest.automatondefinitions.INodeDefinition;
import de.pixida.logtest.designer.Editor;
import de.pixida.logtest.designer.IMainWindow;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;

public class AutomatonEditor extends Editor implements IAutomatonEditor
{
    public static final String AUTOMATON_FILE_MASK = "*.json";
    public static final String AUTOMATON_FILE_DESCRIPTION = "JSON automaton definition (" + AUTOMATON_FILE_MASK + ")";

    private EditorAutomaton automaton;

    private Graph graph;

    private final SplitPane splitPane = new SplitPane();
    private final ScrollPane graphScrollPane = new ScrollPane();

    public AutomatonEditor(final IMainWindow mainWindow)
    {
        super(Editor.Type.AUTOMATON, mainWindow);
        this.setFileMaskAndDescription(AUTOMATON_FILE_MASK, AUTOMATON_FILE_DESCRIPTION);
    }

    @Override
    protected void init()
    {
        this.graph = new Graph(this);

        this.graphScrollPane.setContent(this.graph.getPane());
        this.splitPane.setOrientation(Orientation.HORIZONTAL);

        this.setCenter(this.splitPane);
    }

    @Override
    protected void createNewDocument()
    {
        this.automaton = new EditorAutomaton(this.graph);
        this.automaton.setDescription("<describe the goal of your automaton here>");
        this.initView();

        // Create some existing items not to start on an empty canvas...
        final AutomatonNode newNode = new AutomatonNode(this.graph);
        final double initialNodeX = 30d;
        final double initialNodeY = 70d;
        newNode.setPosition(new Point2D(initialNodeX, initialNodeY));
        newNode.setName("Start");
        newNode.setType(INodeDefinition.Type.INITIAL);
        this.graph.addObject(newNode);
    }

    @Override
    protected void loadDocumentFromFileAndAssignToFile(final File srcFile)
    {
        this.automaton = new EditorAutomaton(this.graph);
        this.automaton.loadFromFile(srcFile);
        this.initView();
        this.assignDocumentToFile(srcFile);
    }

    @Override
    protected void saveDocument()
    {
        Validate.notNull(this.getFile());
        this.automaton.saveToFile(this.getFile());
        this.setChanged(false);
    }

    @Override
    public void handleMinorChange()
    {
        this.setChanged(true);
    }

    @Override
    public void handleChange()
    {
        this.handleMinorChange();

        // TODO: Verify automaton?
    }

    @Override
    public void showConfigFrame(final Node configFrame)
    {
        Validate.notNull(configFrame);
        this.splitPane.getItems().set(1, configFrame);
    }

    @Override
    public void showDefaultConfigFrame()
    {
        this.splitPane.getItems().set(1, this.automaton.getConfigFrame());
    }

    private void initView()
    {
        this.splitPane.getItems().addAll(this.graphScrollPane, this.automaton.getConfigFrame());
        final double maximumPercentage = 0.999;
        this.splitPane.setDividerPositions(maximumPercentage); // Make canvas as big as possible
    }
}
