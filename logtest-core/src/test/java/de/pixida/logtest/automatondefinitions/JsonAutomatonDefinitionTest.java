/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JsonAutomatonDefinitionTest
{
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    public JsonAutomatonDefinitionTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testSuccessfulReadingAnAutomatonDefinition()
    {
        final JsonAutomatonDefinition r = new JsonAutomatonDefinition(
            new File(this.getClass().getResource("test-automaton.json").getFile()));
        final List<INodeDefinition> nodes = r.getNodes();
        final List<IEdgeDefinition> edges = r.getEdges();
        Assert.assertEquals("x = 1;", r.getOnLoad());
        Assert.assertEquals("test automaton", r.getComment());
        Assert.assertEquals("Python", r.getScriptLanguage());
        this.checkNodes(nodes);
        this.checkEdges(nodes, edges);
    }

    @Test
    public void testDisplayNameIsFileNameOfAutomaton()
    {
        final String testFileName = "test-automaton.json";
        final JsonAutomatonDefinition r = new JsonAutomatonDefinition(
            new File(this.getClass().getResource(testFileName).getFile()));
        Assert.assertEquals(testFileName, r.getDisplayName());
        Assert.assertEquals(testFileName, r.toString());
    }

    @Test(expected = AutomatonLoadingException.class)
    public void testExceptionIsThrownWhenSyntaxIsInvalid() throws IOException
    {
        final File rewrittenConfig = this.testFolder.newFile();
        FileUtils.writeStringToFile(rewrittenConfig, "{", JsonAutomatonDefinition.EXPECTED_CHARSET);
        this.triggerLazyLoadingOfDefinitionInFile(rewrittenConfig);
    }

    @Test(expected = AutomatonLoadingException.class)
    public void testExceptionIsThrownIfADestinationNodeIsNotFound() throws IOException
    {
        final JSONObject config = new JSONObject(FileUtils.readFileToString(
            new File(this.getClass().getResource("test-automaton.json").getFile()), JsonAutomatonDefinition.EXPECTED_CHARSET));
        config.getJSONArray("nodes").getJSONObject(0).getJSONArray("outgoingEdges").getJSONObject(0)
            .put("destination", "INVALID_DEST_NODE");
        final File rewrittenConfig = this.testFolder.newFile();
        FileUtils.writeStringToFile(rewrittenConfig, config.toString(), JsonAutomatonDefinition.EXPECTED_CHARSET);
        this.triggerLazyLoadingOfDefinitionInFile(rewrittenConfig);
    }

    @Test(expected = AutomatonLoadingException.class)
    public void testExceptionIsThrownIfARequiredConditionIsInvalid() throws JSONException, IOException
    {
        final JSONObject config = new JSONObject(FileUtils.readFileToString(
            new File(this.getClass().getResource("test-automaton.json").getFile()), JsonAutomatonDefinition.EXPECTED_CHARSET));
        config.getJSONArray("nodes").getJSONObject(0).getJSONArray("outgoingEdges").getJSONObject(0)
            .put("requiredConditions", "INVALID_VALUE");
        final File rewrittenConfig = this.testFolder.newFile();
        FileUtils.writeStringToFile(rewrittenConfig, config.toString(), JsonAutomatonDefinition.EXPECTED_CHARSET);
        this.triggerLazyLoadingOfDefinitionInFile(rewrittenConfig);
    }

    @Test
    public void testWeAreTolerantWhenSourceFileHasInvalidEncoding() throws JSONException, IOException
    {
        final JSONObject config = new JSONObject(FileUtils.readFileToString(
            new File(this.getClass().getResource("test-automaton.json").getFile()), JsonAutomatonDefinition.EXPECTED_CHARSET));
        config.getJSONArray("nodes").getJSONObject(0).put("id", "Ö"); // Chose UTF-8 multibyte character
        final File rewrittenConfig = this.testFolder.newFile();
        FileUtils.writeStringToFile(rewrittenConfig, config.toString(), StandardCharsets.ISO_8859_1);
        this.triggerLazyLoadingOfDefinitionInFile(rewrittenConfig);
    }

    public void checkNodes(final List<INodeDefinition> nodes)
    {
        int i = 0;
        Assert.assertEquals("start", nodes.get(i).toString());
        Assert.assertEquals(EnumSet.of(INodeDefinition.Flag.IS_INITIAL), nodes.get(i).getFlags());
        Assert.assertEquals("x = 10", nodes.get(i).getOnEnter());
        Assert.assertEquals("x++;", nodes.get(i).getOnLeave());
        Assert.assertFalse(nodes.get(i).getWait());
        i++;
        Assert.assertEquals("failing", nodes.get(i).toString());
        Assert.assertEquals(EnumSet.of(INodeDefinition.Flag.IS_FAILURE), nodes.get(i).getFlags());
        Assert.assertNull(nodes.get(i).getOnEnter());
        Assert.assertNull(nodes.get(i).getOnLeave());
        Assert.assertTrue(nodes.get(i).getWait());
        i++;
        Assert.assertEquals("success", nodes.get(i).toString());
        Assert.assertEquals(EnumSet.of(INodeDefinition.Flag.IS_SUCCESS), nodes.get(i).getFlags());
        Assert.assertNotNull(nodes.get(i).getSuccessCheckExp());
        Assert.assertNull(nodes.get(i).getOnEnter());
        Assert.assertNull(nodes.get(i).getOnLeave());
        Assert.assertFalse(nodes.get(i).getWait());
        i++;
        Assert.assertEquals("dummy", nodes.get(i).toString());
        Assert.assertEquals(EnumSet.noneOf(INodeDefinition.Flag.class), nodes.get(i).getFlags());
        Assert.assertNull(nodes.get(i).getOnEnter());
        Assert.assertNull(nodes.get(i).getOnLeave());

        Assert.assertEquals(++i, nodes.size());
    }

    public void checkEdges(final List<INodeDefinition> nodes, final List<IEdgeDefinition> edges)
    {
        this.checkEdgesLinkNodesCorrectly(nodes, edges);

        final Iterator<IEdgeDefinition> edgesIt = edges.iterator();

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
        this.checkTimeInterval(edge);
        Assert.assertEquals(IEdgeDefinition.DEFAULT_CHANNEL, edge.getChannel());
        Assert.assertNull(edge.getTimeIntervalSinceLastTransition());
        Assert.assertNull(edge.getTimeIntervalSinceAutomatonStart());
        Assert.assertNull(edge.getTimeIntervalForEvent());

        edge = edgesIt.next();
        Assert.assertEquals("to_success", edge.toString());
        Assert.assertEquals("[A-Z]*", edge.getRegExp());
        Assert.assertNull(edge.getCheckExp());
        Assert.assertNull(edge.getTriggerAlways());
        Assert.assertNull(edge.getOnWalk());
        Assert.assertFalse(edge.getTriggerOnEof());
        Assert.assertNull(edge.getRequiredConditions());
        Assert.assertNull(edge.getTimeIntervalSinceLastMicrotransition());
        Assert.assertEquals(IEdgeDefinition.DEFAULT_CHANNEL, edge.getChannel());
        Assert.assertNotNull(edge.getTimeIntervalSinceLastTransition());
        Assert.assertNotNull(edge.getTimeIntervalSinceAutomatonStart());
        Assert.assertNotNull(edge.getTimeIntervalForEvent());

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

        Assert.assertFalse(edgesIt.hasNext());
    }

    private void checkEdgesLinkNodesCorrectly(final List<INodeDefinition> nodes, final List<IEdgeDefinition> edges)
    {
        final int firstNode = 0;
        final int secondNode = 1;
        final int thirdNode = 2;
        final int fourthNode = 3;
        final Iterator<IEdgeDefinition> edgesIt = edges.iterator();

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

    public void checkTimeInterval(final IEdgeDefinition edge)
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

    private void triggerLazyLoadingOfDefinitionInFile(final File rewrittenConfig)
    {
        new JsonAutomatonDefinition(rewrittenConfig).getNodes();
    }
}
