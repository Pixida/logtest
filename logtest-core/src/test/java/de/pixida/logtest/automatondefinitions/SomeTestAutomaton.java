/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

abstract class SomeTestAutomaton
{
    public final static String TEST_FILE_NAME = "test-automaton.json";

    static JsonAutomatonDefinition getAutomatonDefinition()
    {
        return new JsonAutomatonDefinition(getTestAutomatonFileResource());
    }

    static JSONObject getRawConfigJson()
    {
        try
        {
            return new JSONObject(FileUtils.readFileToString(getTestAutomatonFileResource(), JsonAutomatonDefinition.EXPECTED_CHARSET));
        }
        catch (JSONException | IOException e)
        {
            throw new RuntimeException("Test automaton definition file not found or invalid syntax!?", e);
        }
    }

    static void verifyAutomatonDefinition(final IAutomatonDefinition testAutomaton)
    {
        testAutomaton.load();
        final List<? extends INodeDefinition> nodes = testAutomaton.getNodes();
        final List<? extends IEdgeDefinition> edges = testAutomaton.getEdges();
        Assert.assertEquals("x = 1;", testAutomaton.getOnLoad());
        Assert.assertEquals("test automaton", testAutomaton.getComment());
        Assert.assertEquals("Python", testAutomaton.getScriptLanguage());
        checkNodes(nodes);
        checkEdges(nodes, edges);
    }

    private static File getTestAutomatonFileResource()
    {
        return new File(SomeTestAutomaton.class.getResource(TEST_FILE_NAME).getFile());
    }

    private static void checkNodes(final List<? extends INodeDefinition> nodes)
    {
        int i = 0;
        Assert.assertEquals("start", nodes.get(i).toString());
        Assert.assertEquals(EnumSet.of(INodeDefinition.Flag.IS_INITIAL), nodes.get(i).getFlags());
        Assert.assertEquals("x = 10", nodes.get(i).getOnEnter());
        Assert.assertEquals("x++;", nodes.get(i).getOnLeave());
        Assert.assertFalse(nodes.get(i).getWait());
        Assert.assertEquals("node_comment", nodes.get(i).getComment());
        i++;
        Assert.assertEquals("failing", nodes.get(i).toString());
        Assert.assertEquals(EnumSet.of(INodeDefinition.Flag.IS_FAILURE), nodes.get(i).getFlags());
        Assert.assertNull(nodes.get(i).getOnEnter());
        Assert.assertNull(nodes.get(i).getOnLeave());
        Assert.assertTrue(nodes.get(i).getWait());
        Assert.assertNull(nodes.get(i).getComment());
        i++;
        Assert.assertEquals("success", nodes.get(i).toString());
        Assert.assertEquals(EnumSet.of(INodeDefinition.Flag.IS_SUCCESS), nodes.get(i).getFlags());
        Assert.assertNotNull(nodes.get(i).getSuccessCheckExp());
        Assert.assertNull(nodes.get(i).getOnEnter());
        Assert.assertNull(nodes.get(i).getOnLeave());
        Assert.assertFalse(nodes.get(i).getWait());
        Assert.assertNull(nodes.get(i).getComment());
        i++;
        Assert.assertEquals("dummy", nodes.get(i).toString());
        Assert.assertEquals(EnumSet.noneOf(INodeDefinition.Flag.class), nodes.get(i).getFlags());
        Assert.assertNull(nodes.get(i).getOnEnter());
        Assert.assertNull(nodes.get(i).getOnLeave());
        Assert.assertNull(nodes.get(i).getComment());

        Assert.assertEquals(++i, nodes.size());
    }

    private static void checkEdges(final List<? extends INodeDefinition> nodes, final List<? extends IEdgeDefinition> edges)
    {
        checkEdgesLinkNodesCorrectly(nodes, edges);

        final Iterator<? extends IEdgeDefinition> edgesIt = edges.iterator();

        IEdgeDefinition edge = edgesIt.next();
        Assert.assertEquals("to_failing", edge.toString());
        Assert.assertNull(edge.getRegExp());
        Assert.assertEquals("x % 2 == 0", edge.getCheckExp());
        Assert.assertNull(edge.getTriggerAlways());
        Assert.assertEquals(
            "engine.info(\"info msg\"); engine.debug(\"debug msg\"); engine.reject(\"We\'re finished\"); engine.getLogEntry();",
            edge.getOnWalk());
        Assert.assertNull(edge.getTriggerOnEof());
        Assert.assertEquals(IEdgeDefinition.RequiredConditions.ONE, edge.getRequiredConditions());
        checkTimeInterval(edge);
        Assert.assertEquals(IEdgeDefinition.DEFAULT_CHANNEL, edge.getChannel());
        Assert.assertNull(edge.getTimeIntervalSinceLastTransition());
        Assert.assertNull(edge.getTimeIntervalSinceAutomatonStart());
        Assert.assertNull(edge.getTimeIntervalForEvent());
        Assert.assertEquals("edge_comment", edge.getComment());

        edge = edgesIt.next();
        Assert.assertEquals("to_success", edge.toString());
        Assert.assertEquals("[A-Z]*", edge.getRegExp());
        Assert.assertNull(edge.getCheckExp());
        Assert.assertNull(edge.getTriggerAlways());
        Assert.assertNull(edge.getOnWalk());
        Assert.assertFalse(BooleanUtils.toBoolean(edge.getTriggerOnEof()));
        Assert.assertNull(edge.getRequiredConditions());
        Assert.assertNull(edge.getTimeIntervalSinceLastMicrotransition());
        Assert.assertEquals(IEdgeDefinition.DEFAULT_CHANNEL, edge.getChannel());
        Assert.assertNotNull(edge.getTimeIntervalSinceLastTransition());
        Assert.assertNotNull(edge.getTimeIntervalSinceAutomatonStart());
        Assert.assertNotNull(edge.getTimeIntervalForEvent());
        Assert.assertNull(edge.getComment());

        edge = edgesIt.next();
        Assert.assertEquals("several_criteria", edge.toString());
        Assert.assertEquals("[^A-Z]", edge.getRegExp());
        Assert.assertNull(edge.getCheckExp());
        Assert.assertTrue(edge.getTriggerAlways());
        Assert.assertNull(edge.getOnWalk());
        Assert.assertTrue(edge.getTriggerOnEof());
        Assert.assertEquals(IEdgeDefinition.RequiredConditions.ALL, edge.getRequiredConditions());
        Assert.assertNull(edge.getTimeIntervalSinceLastMicrotransition());
        Assert.assertEquals("chan", edge.getChannel());
        Assert.assertNull(edge.getTimeIntervalSinceLastTransition());
        Assert.assertNull(edge.getTimeIntervalSinceAutomatonStart());
        Assert.assertNull(edge.getTimeIntervalForEvent());
        Assert.assertNull(edge.getComment());

        Assert.assertFalse(edgesIt.hasNext());
    }

    private static void checkEdgesLinkNodesCorrectly(final List<? extends INodeDefinition> nodes,
        final List<? extends IEdgeDefinition> edges)
    {
        final int firstNode = 0;
        final int secondNode = 1;
        final int thirdNode = 2;
        final int fourthNode = 3;
        final Iterator<? extends IEdgeDefinition> edgesIt = edges.iterator();

        IEdgeDefinition edge = edgesIt.next();
        Assert.assertEquals(nodes.get(firstNode), edge.getSource());
        Assert.assertEquals(nodes.get(secondNode), edge.getDestination());

        edge = edgesIt.next();
        Assert.assertEquals(nodes.get(firstNode), edge.getSource());
        Assert.assertEquals(nodes.get(thirdNode), edge.getDestination());

        edge = edgesIt.next();
        Assert.assertEquals(nodes.get(firstNode), edge.getSource());
        Assert.assertEquals(nodes.get(fourthNode), edge.getDestination());

        Assert.assertFalse(edgesIt.hasNext());
    }

    private static void checkTimeInterval(final IEdgeDefinition edge)
    {
        Assert.assertNotNull(edge.getTimeIntervalSinceLastMicrotransition().getMin());
        Assert.assertNotNull(edge.getTimeIntervalSinceLastMicrotransition().getMax());
        Assert.assertEquals(TimeUnit.SECONDS, edge.getTimeIntervalSinceLastMicrotransition().getMin().getUnit());
        Assert.assertEquals("10", edge.getTimeIntervalSinceLastMicrotransition().getMin().getValue());
        Assert.assertTrue(edge.getTimeIntervalSinceLastMicrotransition().getMin().isInclusive());
        Assert.assertEquals(TimeUnit.MINUTES, edge.getTimeIntervalSinceLastMicrotransition().getMax().getUnit());
        Assert.assertEquals("20", edge.getTimeIntervalSinceLastMicrotransition().getMax().getValue());
        Assert.assertFalse(edge.getTimeIntervalSinceLastMicrotransition().getMax().isInclusive());
    }
}
