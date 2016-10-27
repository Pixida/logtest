/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import de.pixida.logtest.automatondefinitions.GenericNode;
import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.automatondefinitions.INodeDefinition;
import de.pixida.logtest.automatondefinitions.INodeDefinition.Type;
import de.pixida.logtest.automatondefinitions.TestAutomaton;
import de.pixida.logtest.logreaders.GenericLogEntry;

public class AutomatonTest
{
    private static final long TWO = 2;
    private static final long TIME_TO_WAIT_TO_DETECT_ENDLESS_LOOP_MS = 1000;

    private static class LoggerListener extends AppenderSkeleton
    {
        private final Level level;
        private final String str;
        private boolean messageSpotted = false;

        LoggerListener(final Level aLevel, final String aStr)
        {
            this.level = aLevel;
            this.str = aStr;
        }

        @Override
        public void close()
        {
        }

        @Override
        public boolean requiresLayout()
        {
            return false;
        }

        @Override
        protected void append(final LoggingEvent event)
        {
            if (event.getLevel() == this.level && event.getMessage().toString().indexOf(this.str) != -1)
            {
                this.messageSpotted = true;
            }
        }

        boolean getMessageSpotted()
        {
            return this.messageSpotted;
        }
    }

    public AutomatonTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testAutomatonTransitionViaRegexpWorks()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withRegExp("HELLO");

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testAutomatonTransitionViaTimestampWorks()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode middle = ta.createNode().get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, middle).withTimeIntervalSinceLastMicrotransition(0L);
        ta.createEdge(middle, success).withTimeIntervalSinceLastMicrotransition(1L);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "timeout 0 triggers"));
        Assert.assertTrue(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "no timeout triggers as we're not yet one second in state"));
        Assert.assertTrue(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());

        a.proceedWithLogEntry(new GenericLogEntry(1, TWO, "we were one second in state now - timeout triggers"));
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testAutomatonStillWorksWhenTheTimeResets()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode middle = ta.createNode().get();
        final GenericNode middle2 = ta.createNode().get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, middle).withTimeIntervalSinceLastMicrotransition(1L);
        ta.createEdge(middle, middle2).withRegExp("A");
        ta.createEdge(middle2, success).withTimeIntervalSinceLastMicrotransition(1L);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "")); // Go to initial node
        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "")); // Go to middle
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1, 0, "A")); // Reset time, go to middle2
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1 + 1, 1, "")); // Go to success
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testAutomatonTransitionViaAlwaysTriggeringEdges()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode target = ta.createNode().get();
        ta.createEdge(initial, target).withTriggerAlways();

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "f00"));
        Assert.assertFalse(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
    }

    @Test(timeout = TIME_TO_WAIT_TO_DETECT_ENDLESS_LOOP_MS)
    public void testInfiniteLoopsDoNotLeadToEndlessProcessing()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode middle = ta.createNode().get();
        ta.createEdge(initial, middle).withTimeIntervalSinceLastMicrotransition(0L);
        ta.createEdge(middle, initial).withTimeIntervalSinceLastMicrotransition(0L);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "provoke infinite loop"));
        Assert.assertTrue(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
    }

    @Test
    public void testWaitCommandEndsMicrotransitions()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode middle = ta.createNode().withWait().get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, middle).withTriggerAlways();
        ta.createEdge(middle, success).withTriggerAlways();

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "some event"));
        Assert.assertTrue(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "some event"));
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test(timeout = TIME_TO_WAIT_TO_DETECT_ENDLESS_LOOP_MS)
    public void testSelfLoopsDoNotLeadToEndlessProcessing()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(initial, initial).withTimeIntervalSinceLastMicrotransition(0L);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "provoke infinite loop"));
        Assert.assertTrue(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
    }

    @Test
    public void testErrorIsRaisedIfAutomatonHasNoInitialNode()
    {
        final TestAutomaton ta = new TestAutomaton();
        ta.createNode();
        Assert.assertFalse(this.createAutomaton(ta).canProceed());
    }

    @Test
    public void testErrorIsRaisedIfAutomatonHasMultipleInitialNodes()
    {
        final TestAutomaton ta = new TestAutomaton();
        ta.createNode().withType(INodeDefinition.Type.INITIAL);
        ta.createNode().withType(INodeDefinition.Type.INITIAL);
        Assert.assertFalse(this.createAutomaton(ta).canProceed());
    }

    @Test
    public void testErrorIsRaisedIfAFailureNodeHasOutgoingEdges()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode init = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode failure = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(init, failure);
        ta.createEdge(failure, init);
        Assert.assertFalse(this.createAutomaton(ta).canProceed());
    }

    @Test
    public void testNoErrorIsRaisedIfATimeoutValueIsNegative()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode n = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(n, n).withTimeIntervalSinceLastMicrotransition(-1L);
        Assert.assertTrue(this.createAutomaton(ta).canProceed());
    }

    @Test
    public void testErrorIsRaisedIfARegularExpressionIsInvalid()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode n = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(n, n).withRegExp("[");
        Assert.assertFalse(this.createAutomaton(ta).canProceed());
    }

    @Test
    public void testErrorIsRaisedIfAnEdgeHasNoCondition()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode n = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(n, n);
        final Automaton a = this.createAutomaton(ta);
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.automatonDefect());
    }

    @Test
    public void testErrorIsRaisedIfASourceNodeDoesNotExist()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode n = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode unregisteredNode = new GenericNode("");
        ta.createEdge(unregisteredNode, n);
        Assert.assertFalse(this.createAutomaton(ta).canProceed());
    }

    @Test
    public void testErrorIsRaisedIfADestinationNodeDoesNotExist()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode n = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode unregisteredNode = new GenericNode("");
        ta.createEdge(n, unregisteredNode);
        Assert.assertFalse(this.createAutomaton(ta).canProceed());
    }

    @Test(expected = ExecutionException.class)
    public void testErrorIsRaisedDuringProcessingWhenThereAreMultipleMatchingEdgesToMultipleNodes()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode one = ta.createNode().get();
        final GenericNode other = ta.createNode().get();
        ta.createEdge(initial, one).withTriggerAlways();
        ta.createEdge(initial, other).withTriggerAlways();

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "provoke ambiguous decision"));
    }

    @Test
    public void testNoErrorIsRaisedDuringProcessingWhenThereAreMultipleMatchingEdgesToSameNode()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode one = ta.createNode().get();
        ta.createEdge(initial, one).withTriggerAlways();
        ta.createEdge(initial, one).withTriggerAlways();

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "provoke ambiguous decision"));
    }

    @Test(expected = ExecutionException.class)
    public void testErrorIsRaisedDuringProcessingWhenThereAreMultipleMatchingEdgesToSameNodeButHaveScriptingActions()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode one = ta.createNode().get();
        ta.createEdge(initial, one).withTriggerAlways();
        ta.createEdge(initial, one).withTriggerAlways().withOnWalk("C++");

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "provoke ambiguous decision"));
    }

    @Test
    public void testLoggingFromScriptWorks()
    {
        final String logMagicDebug = "DEBUG_DADJASLODSA";
        final String logMagicInfo = "INFO_ADAHDSAIUHYE";
        final LoggerListener llDebug = new LoggerListener(Level.DEBUG, logMagicDebug);
        final LoggerListener llInfo = new LoggerListener(Level.INFO, logMagicInfo);
        try
        {
            Logger.getLogger(Automaton.class).addAppender(llDebug);
            Logger.getLogger(Automaton.class).addAppender(llInfo);

            final TestAutomaton ta = new TestAutomaton().withOnLoad("engine.debug('" + logMagicDebug + "\\nLinefeedtest');"
                + "engine.info('" + logMagicInfo + "\\nLinefeedtest');");
            ta.createNode().withType(INodeDefinition.Type.INITIAL);

            final Automaton a = this.createAndCheckAutomaton(ta);

            a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));

            Assert.assertTrue(llDebug.getMessageSpotted());
            Assert.assertTrue(llInfo.getMessageSpotted());
        }
        finally
        {
            Logger.getLogger(Automaton.class).removeAppender(llDebug);
            Logger.getLogger(Automaton.class).removeAppender(llInfo);
        }
    }

    @Test
    public void testThereIsNoLogEntryDataDuringOnLoad()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("if (engine.getLogEntryTime() != null "
            + "|| engine.getLogEntryPayload() != null || engine.getLogEntryLineNumber() != null) engine.reject()");

        ta.createNode().withType(INodeDefinition.Type.INITIAL);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
    }

    @Test
    public void testThereIsCorrectLogEntryDataAfterOnLoad()
    {
        final TestAutomaton ta = new TestAutomaton();

        ta.createNode().withType(INodeDefinition.Type.INITIAL).withOnEnter("if (engine.getLogEntryTime() != 0 "
            + "|| engine.getLogEntryPayload() != 'peek' || engine.getLogEntryLineNumber() != 1) engine.reject()");

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
    }

    @Test
    public void testThatScriptsAreExecutedProperly()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("str = 'A'");

        final GenericNode initial = ta.createNode()
            .withType(INodeDefinition.Type.INITIAL)
            .withOnEnter("str += 'B'").withOnLeave("str += 'C'").get();
        final GenericNode other = ta.createNode().withOnEnter("str += 'E'").get();
        final GenericNode finish = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, other).withTriggerAlways().withOnWalk("str += 'D'");
        ta.createEdge(other, finish).withCheckExp("str == 'ABCDE'");

        final Automaton a = this.createAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertFalse(a.canProceed());
        Assert.assertFalse(a.automatonDefect());
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testHaltOnInitWorks()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("engine.halt('Halting in onLoad handler')");

        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withTriggerAlways();

        final Automaton a = this.createAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertFalse(a.succeeded());
    }

    @Test
    public void testRejectOnInitWorks()
    {
        final String rejectMsg = "ADJK";
        final TestAutomaton ta = new TestAutomaton().withOnLoad("engine.reject('" + rejectMsg + "')");

        final GenericNode initial = ta.createNode()
            .withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode failure = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, failure).withTriggerAlways();

        final Automaton a = this.createAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertFalse(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertTrue(a.getErrorReason().contains(rejectMsg));
    }

    @Test(expected = ExecutionException.class)
    public void testCallingRejectAndAcceptThrowsAnError()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("engine.reject(); engine.accept();");

        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode failure = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, failure).withTriggerAlways();

        final Automaton a = this.createAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
    }

    @Test
    public void testHaltOnOnWalkIsRegardedAfterTheEdgeHasBeenWalked()
    {
        final TestAutomaton ta = new TestAutomaton();

        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withTriggerAlways().withOnWalk("engine.halt()");
        ta.createEdge(success, initial).withTriggerAlways();

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testAcceptOnInitWorks()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("engine.accept('Accepting in onLoad handler')");

        ta.createNode().withType(INodeDefinition.Type.INITIAL);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testAcceptWorks()
    {
        final TestAutomaton ta = new TestAutomaton();

        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode failure = ta.createNode().withType(INodeDefinition.Type.FAILURE).get();
        ta.createEdge(initial, failure).withTriggerAlways().withOnWalk("engine.accept()");

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertTrue(a.succeeded());
    }

    @Test(expected = ExecutionException.class)
    public void testErrorIsThrownWhenScriptTriesToAccessInvalidParameter()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("engine.getParameter('walk')");
        ta.createNode().withType(INodeDefinition.Type.INITIAL);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
    }

    @Test
    public void testParametersAreAlreadyAccessibleInOnLoad()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("engine.getParameter('walk')");
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withTriggerAlways();

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("walk", "true");
        final Automaton a = this.createAutomaton(ta, parameters);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testParametersAreAccessibleFromScripts()
    {
        final TestAutomaton ta = new TestAutomaton();

        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withCheckExp("engine.getParameter('walk') == 'true'");

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("walk", "true");
        final Automaton a = this.createAndCheckAutomaton(ta, parameters);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testParametersAreConsidered()
    {
        final TestAutomaton ta = new TestAutomaton();

        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode node2 = ta.createNode().get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, node2).withRegExp("a${char}a");
        ta.createEdge(node2, success).withTimeIntervalSinceLastMicrotransition("${timeout}");

        final Map<String, String> parameters = new HashMap<>();
        final long timeoutTestValue = 10;
        parameters.put("char", "A");
        parameters.put("timeout", String.valueOf(timeoutTestValue));
        final Automaton a = this.createAndCheckAutomaton(ta, parameters);

        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
        Assert.assertFalse(a.succeeded());
        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "aAa"));
        Assert.assertFalse(a.succeeded());
        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "f00"));
        Assert.assertFalse(a.succeeded());
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1, timeoutTestValue, "f002"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testLoadingFailsWhenAParameterIsNotSet()
    {
        final TestAutomaton ta = new TestAutomaton();

        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(initial, ta.createNode().get()).withRegExp("${char}");

        Assert.assertNotNull(this.createAutomaton(ta).getErrorReason());
    }

    @Test
    public void testLoadingFailsWhenATimeoutIsNotNumeric()
    {
        final TestAutomaton ta = new TestAutomaton();

        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(initial, ta.createNode().get()).withTimeIntervalSinceLastMicrotransition("${longValue}");
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("longValue", "non numeric value");
        Assert.assertNotNull(this.createAutomaton(ta, parameters).getErrorReason());
    }

    @Test
    public void testLoadingFailsIfAScriptContainsErrors()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("(");

        ta.createNode().withType(INodeDefinition.Type.INITIAL);

        Assert.assertNotNull(this.createAutomaton(ta).getErrorReason());
    }

    @Test
    public void testSuccessfullyCreatedAutomatonUseInitialStateEvenIfNoInputWasProcessed()
    {
        final TestAutomaton ta = new TestAutomaton();
        ta.createNode().withType(INodeDefinition.Type.INITIAL);
        Assert.assertFalse(this.createAutomaton(ta).succeeded());
    }

    @Test(expected = ExecutionException.class)
    @Ignore // Ignored until this is solved
    public void testThatScriptsRunSandboxed()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("var File = java.io.File;\n"
            + "// list contents of the current directory!\n"
            + "for each (var f in new File(\".\").list())\n"
            + "print(f)");
        ta.createNode().withType(INodeDefinition.Type.INITIAL);
        final Automaton a = this.createAndCheckAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "peek"));
    }

    @Test
    public void testInitialStateIsNotEvaluatedAsSuccess()
    {
        final TestAutomaton ta = new TestAutomaton();
        ta.createNode().withType(INodeDefinition.Type.INITIAL);
        final Automaton a = this.createAndCheckAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "f00"));
        Assert.assertFalse(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
    }

    @Test
    public void testANodeWithoutStateIsNotEvaluatedAsSuccess()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode noType = ta.createNode().get();
        ta.createEdge(initial, noType).withTriggerAlways();
        final Automaton a = this.createAndCheckAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "f00"));
        Assert.assertFalse(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
    }

    @Test
    public void testAFailureNodeIsNotEvaluatedAsSuccess()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode failure = ta.createNode().withType(INodeDefinition.Type.FAILURE).get();
        ta.createEdge(initial, failure).withTriggerAlways();
        final Automaton a = this.createAndCheckAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "f00"));
        Assert.assertFalse(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
    }

    @Test
    public void testASuccessNodeIsEvaluatedAsSuccess()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withTriggerAlways();
        final Automaton a = this.createAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(1, 0, "f00"));
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testObtainingMatchingRegExpOfEdgeConditionWorks()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL)
            .withOnEnter("if (engine.getRegExpConditionMatchingGroups(0) != null) engine.reject('0')")
            .withOnLeave("if (engine.getRegExpConditionMatchingGroups(0) != null) engine.reject('1')").get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS)
            .withOnEnter("if (engine.getRegExpConditionMatchingGroups(0) != null) engine.reject('2')")
            .withOnLeave("if (engine.getRegExpConditionMatchingGroups(0) != null) engine.reject('3')").get();
        ta.createEdge(initial, success).withRegExp("HE(L)LO").withOnWalk(
            "if (engine.getRegExpConditionMatchingGroups(0) != 'HELLO' || engine.getRegExpConditionMatchingGroups(1) != 'L')"
                + "engine.reject(engine.getRegExpConditionMatchingGroups(0) + ', ' + engine.getRegExpConditionMatchingGroups(1))");

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test(expected = ExecutionException.class)
    public void testObtainingMatchingRegExpOfEdgeConditionThrowsExceptionIfGroupIndexIsInvalid()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withRegExp("HE(L)LO").withOnWalk("engine.getRegExpConditionMatchingGroups(2);");

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
    }

    @Test
    public void testACheckConditionThrowsAValidationErrorIfNotOnASucceedingState()
    {
        final TestAutomaton ta = new TestAutomaton();
        ta.createNode().withType(INodeDefinition.Type.INITIAL).withSuccessCheckExp("false");
        final Automaton a = this.createAutomaton(ta);
        Assert.assertTrue(a.automatonDefect());
    }

    @Test
    public void testSuccessCheckExpressionIsEvaluatedToSucceed()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).withSuccessCheckExp("true")
            .get();
        ta.createEdge(initial, success).withTriggerAlways();
        final Automaton a = this.createAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
        Assert.assertFalse(a.automatonDefect());
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testSuccessCheckExpressionIsEvaluatedToFail()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).withSuccessCheckExp("false")
            .get();
        ta.createEdge(initial, success).withTriggerAlways();
        final Automaton a = this.createAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
        Assert.assertFalse(a.automatonDefect());
        Assert.assertFalse(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
    }

    @Test
    public void testEofEventIsProcessed()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withEofCondition();

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
        Assert.assertTrue(a.canProceed());
        Assert.assertFalse(a.succeeded());

        a.pushEof();
        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testEvaluationWorksIfAllConditionsMustMatch()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        final GenericNode error = ta.createNode().withType(INodeDefinition.Type.FAILURE).get();
        ta.createEdge(initial, success)
            .withRegExp("HELLO").withCheckExp("true")
            .withRequiredConditions(IEdgeDefinition.RequiredConditions.ALL);
        ta.createEdge(success, error)
            .withRegExp("H").withCheckExp("false")
            .withRequiredConditions(IEdgeDefinition.RequiredConditions.ALL);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1, 1, "H WORLD!"));

        Assert.assertTrue(a.canProceed());
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testEvaluationWorksIfOneConditionsMustMatch()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success)
            .withRegExp("HELLO").withCheckExp("false")
            .withRequiredConditions(IEdgeDefinition.RequiredConditions.ONE);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));

        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testByDefinitionAllConditionsMustMatch()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        final GenericNode error = ta.createNode().withType(INodeDefinition.Type.FAILURE).get();
        ta.createEdge(initial, success).withRegExp("HELLO").withCheckExp("true");
        ta.createEdge(success, error).withRegExp("H").withCheckExp("false");

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1, 1, "H WORLD!"));

        Assert.assertTrue(a.canProceed());
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testTransitionWorksWhenAllConditionsMustMatchAndThereIsAnEofEvent()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode error = ta.createNode().get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, error)
            .withRegExp("HELLO").withCheckExp("true")
            .withRequiredConditions(IEdgeDefinition.RequiredConditions.ALL);
        ta.createEdge(error, success)
            .withRegExp("H").withCheckExp("false").withEofCondition()
            .withRequiredConditions(IEdgeDefinition.RequiredConditions.ALL);

        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));
        a.pushEof();

        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testTimeIntervalSinceLastMicrotransitionIsEvaluated()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        final long minTime = 10L;
        final long maxTime = 30L;
        final long maxTimeMs = 30000;
        ta.createEdge(initial, success).withTimeIntervalSinceLastMicrotransitionMin("" + minTime, TimeUnit.MILLISECONDS, true)
            .withTimeIntervalSinceLastMicrotransitionMax("" + maxTime, TimeUnit.SECONDS, false);

        long line = 1;
        Automaton a = this.createAndCheckAutomaton(ta);
        long t = minTime - 1;
        a.proceedWithLogEntry(new GenericLogEntry(line++, t, ""));
        Assert.assertFalse(a.succeeded());
        t += minTime;
        a.proceedWithLogEntry(new GenericLogEntry(line++, t, ""));
        Assert.assertTrue(a.succeeded());

        line = 1;
        a = this.createAndCheckAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(line++, 0, ""));
        a.proceedWithLogEntry(new GenericLogEntry(line++, minTime + 1, ""));
        Assert.assertTrue(a.succeeded());

        line = 1;
        a = this.createAndCheckAutomaton(ta);
        a.proceedWithLogEntry(new GenericLogEntry(line++, 0, ""));
        a.proceedWithLogEntry(new GenericLogEntry(line++, maxTimeMs - 1, ""));
        Assert.assertFalse(a.succeeded());

        line = 1;
        a = this.createAndCheckAutomaton(ta);
        t = maxTimeMs;
        a.proceedWithLogEntry(new GenericLogEntry(line++, t, ""));
        Assert.assertFalse(a.succeeded());
        t += maxTimeMs + 1;
        a.proceedWithLogEntry(new GenericLogEntry(line++, t, ""));
        Assert.assertFalse(a.succeeded());
    }

    @Test
    public void testTimeSinceLastMicrotransitionCanContainParameters()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withTimeIntervalSinceLastMicrotransition("${t}");
        final long t = 5;
        final Map<String, String> params = new HashMap<>();
        params.put("t", "" + t);
        final Automaton a = this.createAndCheckAutomaton(ta, params);
        a.proceedWithLogEntry(new GenericLogEntry(0, 0, ""));
        Assert.assertTrue(a.canProceed());
        Assert.assertFalse(a.automatonDefect());
        Assert.assertFalse(a.succeeded());
        a.proceedWithLogEntry(new GenericLogEntry(0, t, ""));
        Assert.assertFalse(a.canProceed());
        Assert.assertFalse(a.automatonDefect());
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testNegativeTimeIntervalsSinceLastMicrotransitionAreAccepted()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(initial, initial).withTimeIntervalSinceLastMicrotransitionMin("-1", TimeUnit.MILLISECONDS, true);
        Assert.assertFalse(this.createAutomaton(ta).automatonDefect());
    }

    @Test
    public void testMissingUnitInTimeIntervalsSinceLastMicrotransitionAreDetected()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(initial, initial).withTimeIntervalSinceLastMicrotransitionMin("0", null, true);
        Assert.assertTrue(this.createAutomaton(ta).automatonDefect());
    }

    @Test
    public void testInvalidOrderingInTimeIntervalsSinceLastMicrotransitionAreDetected()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(initial, initial).withTimeIntervalSinceLastMicrotransitionMin("1", TimeUnit.DAYS, true)
            .withTimeIntervalSinceLastMicrotransitionMax("0", TimeUnit.DAYS, true);
        Assert.assertTrue(this.createAutomaton(ta).automatonDefect());
    }

    @Test
    public void testEqualBoundsInTimeIntervalsSinceLastMicrotransitionAreDetected()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        // Example: t \n [3, 3) never is true for any t as t \in [a, a) <=> t >= a && t < a
        ta.createEdge(initial, initial).withTimeIntervalSinceLastMicrotransitionMin("1", TimeUnit.MINUTES, true)
            .withTimeIntervalSinceLastMicrotransitionMax("1", TimeUnit.MINUTES, false);
        Assert.assertTrue(this.createAutomaton(ta).automatonDefect());
    }

    @Test
    public void testEmptySetInTimeIntervalsSinceLastMicrotransitionAreDetected()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        ta.createEdge(initial, initial).withTimeIntervalSinceLastMicrotransitionMin("0", TimeUnit.MILLISECONDS, false)
            .withTimeIntervalSinceLastMicrotransitionMax("1", TimeUnit.MILLISECONDS, false);
        Assert.assertTrue(this.createAutomaton(ta).automatonDefect());
    }

    @Test
    public void testParametersInAutomatonDescriptionAreReplaced()
    {
        final Map<String, String> numberIsFive = new HashMap<>();
        numberIsFive.put("number", "5");
        Assert.assertEquals("5", this.createAutomaton(new TestAutomaton().withDescription("${number}"), numberIsFive).getDescription());
    }

    @Test
    public void testNodeNamesCanContainParameters()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode("Hi${number}!").withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode second = ta.createNode().get();
        ta.createEdge(initial, second).withEofCondition();

        // Currently the description appears only in the log output - grab it. Maybe there is a more convenient way to get it later.
        final LoggerListener phisher = new LoggerListener(Level.DEBUG, "Hi5!");
        try
        {
            Logger.getLogger(Automaton.class).addAppender(phisher);

            final Map<String, String> numberIsFive = new HashMap<>();
            numberIsFive.put("number", "5");
            final Automaton a = this.createAutomaton(ta, numberIsFive);
            a.pushEof();
        }
        finally
        {
            Logger.getLogger(Automaton.class).removeAppender(phisher);
        }

        Assert.assertTrue(phisher.getMessageSpotted());
    }

    @Test
    public void testChannelsWork()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        final GenericNode failure = ta.createNode().withType(INodeDefinition.Type.FAILURE).get();
        ta.createEdge(initial, success).withTriggerAlways().withChannel("A");
        ta.createEdge(initial, failure).withTriggerAlways().withChannel("B");
        ta.createEdge(success, failure).withTriggerAlways().withChannel("B");
        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!", "A"));
        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "HELLO WORLD!", "A"));

        Assert.assertTrue(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testEofWorksEvenForChanneledEdges()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withChannel("XXX").withEofCondition();
        final Automaton a = this.createAndCheckAutomaton(ta);

        a.pushEof();

        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testEofEventIsNotTriggeringTheAlwaysMatchingConditionAsThatIsVeryConfusingWhenDesigningAutomatons()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withTriggerAlways();

        final Automaton a = this.createAutomaton(ta);
        a.pushEof();

        Assert.assertTrue(a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
    }

    @Test
    public void testEmptyLogFilesCanBeExplicitlyDeclaredAsSuccessByUsingEofCondition()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withEofCondition();

        final Automaton a = this.createAutomaton(ta);
        a.pushEof();

        Assert.assertFalse(a.canProceed());
        Assert.assertTrue(a.succeeded());
        Assert.assertNull(a.getErrorReason());
    }

    @Test
    public void testTimingByLastTransitionTimeWorks()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode middle = ta.createNode().get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, middle).withRegExp("INITIAL_TO_MIDDLE");
        ta.createEdge(middle, initial).withRegExp("MIDDLE_TO_INITIAL");
        final long dt = 8;
        ta.createEdge(middle, success).withTimeIntervalSinceLastTransition("" + dt);
        final Automaton a = this.createAndCheckAutomaton(ta);

        final long t1 = 5;
        assert t1 < dt; // Check it does not count t0 as transition
        a.proceedWithLogEntry(new GenericLogEntry(0, 0, "init-timing-at-0"));
        a.proceedWithLogEntry(new GenericLogEntry(1, t1, "INITIAL_TO_MIDDLE MIDDLE_TO_INITIAL")); // Microtransition but no transition
        Assert.assertFalse(a.succeeded());
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1, dt, "INITIAL_TO_MIDDLE"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testTimingByEventTimeWorks()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        final long dt = 8;
        ta.createEdge(initial, success).withTimeIntervalForEvent("" + dt);
        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, 1, "x"));
        Assert.assertFalse(a.succeeded());
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1, dt, "x"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testTimingByAutomatonStartTimeWorks()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        final long t0 = 1000;
        final long dt = 8;
        ta.createEdge(initial, success).withTimeIntervalSinceAutomatonStart("" + dt);
        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(1, t0, "x"));
        Assert.assertFalse(a.succeeded());
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1, dt, "x"));
        Assert.assertFalse(a.succeeded());
        a.proceedWithLogEntry(new GenericLogEntry(1 + 1 + 1, t0 + dt, "x"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testScriptsUseSameVariableScope()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("x = 0; x++;");
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL)
            .withOnEnter("x++").withOnLeave("x++").get();
        final GenericNode scriptsCompleted = ta.createNode().get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, scriptsCompleted).withTriggerAlways().withOnWalk("x++");
        ta.createEdge(scriptsCompleted, success).withCheckExp("x == 4");
        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(0, 0, "x"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testPythonScripting()
    {
        final TestAutomaton ta = new TestAutomaton().withOnLoad("a = 5").withScriptLanguage("python");
        final GenericNode initial = ta.createNode().withType(INodeDefinition.Type.INITIAL).get();
        final GenericNode success = ta.createNode().withType(INodeDefinition.Type.SUCCESS).get();
        ta.createEdge(initial, success).withCheckExp("isinstance(a, int)");
        final Automaton a = this.createAndCheckAutomaton(ta);

        a.proceedWithLogEntry(new GenericLogEntry(0, 0, "x"));
        Assert.assertTrue(a.succeeded());
    }

    @Test
    public void testJavaScriptEvaluatesValuesReturnedByEngineCorrectly()
    {
        // This bug occurred with JavaScript only (with python, it always worked well):
        // After assigning the log entry line number "83" to a JavaScript variable, the later log line number 100 is evaluated as not being
        // greater than the assigned value 83, i.e. "100 > 83" evaluates to "false".
        // Note that writing "engine.getLogEntryLineNumber() > cmp" (100 > 83) evaluated to false, but
        // "engine.getLogEntryLineNumber() > 83" and "100 > cmp" and "100 > 83" evaluate to true. It worked with "cmp" values smaller 100,
        // failed for a lot of numbers >100, but started to evaluate correctly again for much higher numbers, so the behavior was quite
        // unstable and is hard to describe.
        // It was fixed by not returning "long" but "int" values from Java methods into JavaScript. Obviously, the nashorns conversion
        // from "long" to "int" creates broken objects for specific values (e.g. 100). Writing parseInt() around the left or the right
        // expression or the variable during assignment always fixes the problem.
        final TestAutomaton ta = new TestAutomaton().withScriptLanguage("JavaScript");
        final GenericNode initial = ta.createNode("initial").withType(Type.INITIAL).get();
        final GenericNode ok = ta.createNode("ok").withType(Type.SUCCESS).get();
        ta.createEdge(initial, ok).withCheckExp(
            "if (engine.getLogEntryLineNumber() == 83) cmp = engine.getLogEntryLineNumber();"
                + "engine.getLogEntryLineNumber() == 100 && engine.getLogEntryLineNumber() > cmp");
        final Automaton automaton = new Automaton(ta, new HashMap<>());
        for (int i = 1; i <= 100; i++)
        {
            automaton.proceedWithLogEntry(new GenericLogEntry(i, 0, ""));
        }
        // HACK: Normally, the automaton *MUST* succeed. Currently, we leave this bug as it will hopefully be fixed. The solution is
        // to always write Number() when accessing "long" values from JavaScript.
        Assert.assertFalse(automaton.succeeded()); // If this assumption fails, the bug has been fixed and engine methods returning "long"
        // can be called without writing Number(engine.getXYZ())
    }

    private Automaton createAndCheckAutomaton(final TestAutomaton ta)
    {
        return this.createAndCheckAutomaton(ta, Collections.emptyMap());
    }

    private Automaton createAndCheckAutomaton(final TestAutomaton ta, final Map<String, String> params)
    {
        final Automaton a = this.createAutomaton(ta, params);
        Assert.assertTrue(ta.getNodes().size() == 1 || a.canProceed());
        Assert.assertFalse(a.succeeded());
        Assert.assertNotNull(a.getErrorReason());
        Assert.assertFalse(a.automatonDefect());
        return a;
    }

    private Automaton createAutomaton(final TestAutomaton ta, final Map<String, String> params)
    {
        return new Automaton(ta, params);
    }

    private Automaton createAutomaton(final TestAutomaton ta)
    {
        return this.createAutomaton(ta, Collections.emptyMap());
    }
}
