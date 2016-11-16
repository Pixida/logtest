/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.automatondefinitions.AutomatonLoadingException;
import de.pixida.logtest.automatondefinitions.IAutomatonDefinition;
import de.pixida.logtest.automatondefinitions.IEdgeDefinition;
import de.pixida.logtest.automatondefinitions.INodeDefinition;
import de.pixida.logtest.engine.AutomatonNode.Type;
import de.pixida.logtest.engine.conditions.IEventDescription;
import de.pixida.logtest.engine.conditions.IScriptEnvironment;
import de.pixida.logtest.logreaders.ILogEntry;

public class Automaton
{
    public static final String DEFAULT_SCRIPTING_LANGUAGE = "JavaScript";

    private static final Logger LOG = LoggerFactory.getLogger(Automaton.class);

    private final class EventForConditionEvaluation implements IEventDescription
    {
        private EventForConditionEvaluation()
        {
            // Empty constructor needed by checkstyle
        }

        @Override
        public boolean isLogEntry()
        {
            return Automaton.this.currentEvent.isLogEntry();
        }

        @Override
        public long getLogEntryTime()
        {
            return Automaton.this.currentEvent.getLogEntry().getTime();
        }

        @Override
        public boolean isEof()
        {
            return Automaton.this.currentEvent.isEof();
        }

        @Override
        public String getLogEntryPayload()
        {
            return Automaton.this.currentEvent.getLogEntry().getPayload();
        }

        @Override
        public String getChannel()
        {
            if (Automaton.this.currentEvent.getLogEntry() == null)
            {
                return ILogEntry.DEFAULT_CHANNEL;
            }
            else
            {
                return Automaton.this.currentEvent.getLogEntry().getChannel();
            }
        }

        @Override
        public boolean getSupportsChannels()
        {
            return Automaton.this.currentEvent.getLogEntry() != null;
        }
    }

    public class ScriptEnvironment implements IScriptEnvironment
    // Make sure all methods which should be callable from a script are public, otherwise it won't work!
    {
        private static final String SCRIPT_LOG_OUTPUT_FORMAT = "Script output: {}";

        // Note the current logEntry information may be null when the onLoad-Action is executed
        private Long time;
        private String payload;
        private Long lineNumber;
        private List<String> regExpConditionMatchingGroups;

        ScriptEnvironment()
        {
            // Empty constructor needed by checkstyle
        }

        public void info(final String msg)
        {
            for (final String line : this.splitLogLinesFromScript(msg))
            {
                LOG.info(SCRIPT_LOG_OUTPUT_FORMAT, line);
            }
        }

        public void debug(final String msg)
        {
            for (final String line : this.splitLogLinesFromScript(msg))
            {
                LOG.debug(SCRIPT_LOG_OUTPUT_FORMAT, line);
            }
        }

        public void reject(final String msg)
        {
            LOG.debug("Script requested to reject with message: {}", msg);
            Automaton.this.rejectFlag = true;
            Automaton.this.rejectMessage = msg;
            this.checkOnlyEitherRejectOrAcceptAreSet();
        }

        public void reject()
        {
            LOG.debug("Script requested to reject without message");
            Automaton.this.rejectFlag = true;
            Automaton.this.rejectMessage = null;
            this.checkOnlyEitherRejectOrAcceptAreSet();
        }

        public void halt(final String msg)
        {
            LOG.debug("Script requested to halt with message: {}", msg);
            Automaton.this.haltFlag = true;
        }

        public void halt()
        {
            LOG.debug("Script requested to halt without message");
            Automaton.this.haltFlag = true;
        }

        public void accept(final String msg)
        {
            LOG.debug("Script requested to accept with message: {}", msg);
            Automaton.this.acceptFlag = true;
            this.checkOnlyEitherRejectOrAcceptAreSet();
        }

        public void accept()
        {
            LOG.debug("Script requested to accept without message");
            Automaton.this.acceptFlag = true;
            this.checkOnlyEitherRejectOrAcceptAreSet();
        }

        public String getParameter(final String name)
        {
            final String value = Automaton.this.parameters.get(name);
            if (value == null)
            {
                LOG.error("Script requested parameter '{}' which was not set!", name);
                throw new ExecutionException("Script requested parameter which is not set: " + name);
            }
            else
            {
                return value;
            }
        }

        public Long getLogEntryTime()
        {
            return this.time;
        }

        public String getLogEntryPayload()
        {
            return this.payload;
        }

        public Long getLogEntryLineNumber()
        {
            return this.lineNumber;
        }

        public String getRegExpConditionMatchingGroups(final int group)
        {
            LOG.trace("Script requesting reg exp matching group {}", group);
            if (this.regExpConditionMatchingGroups == null)
            {
                LOG.trace("No reg exp matching group available");
                return null;
            }
            if (group < 0 || group >= this.regExpConditionMatchingGroups.size())
            {
                LOG.error("Invalid index '{}' for accessing matches of regular expression condition '{}'",
                    group, this.regExpConditionMatchingGroups);
                throw new ExecutionException("Cannot get matching group as index is out of bounds: " + group);
            }
            return this.regExpConditionMatchingGroups.get(group);
        }

        private void checkOnlyEitherRejectOrAcceptAreSet()
        {
            if (Automaton.this.acceptFlag && Automaton.this.rejectFlag)
            {
                final String msg = "reject() and accept() were called from scripts at the same time!";
                LOG.error(msg);
                throw new ExecutionException(msg);
            }
        }

        private String[] splitLogLinesFromScript(final String msg)
        {
            return msg.split("(\r|\n|\r\n)");
        }

        void updateEnvironment(final Event event)
        {
            LOG.trace("Updating scripting environment");
            this.time = event.isLogEntry() ? event.getLogEntry().getTime() : null;
            this.payload = event.isLogEntry() ? event.getLogEntry().getPayload() : null;
            this.lineNumber = event.isLogEntry() ? event.getLogEntry().getLineNumber() : null;
        }

        @Override
        public void setRegExpConditionMatchingGroups(final List<String> value)
        {
            LOG.trace("Reg exp matching group set to: {}", value);
            this.regExpConditionMatchingGroups = value;
        }
    }

    private class LastTransition
    {
        private final AutomatonEdge edge;
        private final Event event;
        private final long timeMs;

        LastTransition(final AutomatonEdge aEdge, final Event aEvent, final long aTimeMs)
        {
            this.edge = aEdge;
            this.event = aEvent;
            this.timeMs = aTimeMs;
        }

        AutomatonNode getSourceNode()
        {
            return this.edge.getSourceNode();
        }

        Event getEvent()
        {
            return this.event;
        }

        AutomatonEdge getEdge()
        {
            return this.edge;
        }

        long getTimeMs()
        {
            return this.timeMs;
        }
    }

    private enum NodeSuccessState
    {
        SUCCESS,
        NON_SUCCESS_NODE,
        SUCCESS_NODE_BUT_SUCCESS_CONDITION_FAILED
    }

    private final IAutomatonDefinition automatonDefinition;
    private EmbeddedScript onLoad;
    private final List<AutomatonNode> nodes = new ArrayList<>();
    private final List<AutomatonEdge> edges = new ArrayList<>();
    private AutomatonNode initialNode;
    private AutomatonNode currentNode;
    private final LinkedList<AutomatonNode> visitedNodes = new LinkedList<>(); // Used during processing, cached as member variable
    private Throwable thrownException;
    private ScriptEngine scriptingEngine;
    private ScriptEnvironment scriptEnvironment;
    private final AutomatonParameters parameters;
    private boolean haltFlag;
    private boolean acceptFlag;
    private boolean rejectFlag;
    private String rejectMessage;
    private LastTransition lastTransition;
    private Event currentEvent;
    private String description;
    private String scriptLanguage;
    private final TimingInfo timingInfo = new TimingInfo();

    public Automaton(final IAutomatonDefinition aAutomatonDefinition, final Map<String, String> aParameters)
    {
        LOG.debug("Creating automaton with definition '{}' and parameters '{}'", aAutomatonDefinition, aParameters);
        Validate.notNull(aAutomatonDefinition);
        Validate.notNull(aParameters);
        this.automatonDefinition = aAutomatonDefinition;
        this.parameters = new AutomatonParameters(aParameters);
        try
        {
            this.loadAutomatonFromDefinition();
            if (this.description != null)
            {
                this.description = this.parameters.insertAllParameters(this.description);
            }
            LOG.debug("Automaton description: {}", this.description);
            this.checkAutomatonAndFindInitialNode();
            this.compileScripts();
        }
        catch (final InvalidAutomatonDefinitionException iade)
        {
            final String errorsWithoutStackTraces = ExceptionUtils.getThrowableList(iade).stream().map(e -> e.getMessage())
                .collect(Collectors.joining("; "));
            LOG.error("Error while initializing automaton '{}': {}", this.automatonDefinition, errorsWithoutStackTraces);
            this.thrownException = iade;
        }
        catch (final RuntimeException re)
        {
            this.thrownException = re;
            throw re;
        }
    }

    public void proceedWithLogEntry(final ILogEntry logEntry)
    {
        LOG.trace("Proceeding with log entry: {}", logEntry);
        Validate.notNull(logEntry);
        Validate.notNull(logEntry.getPayload());
        this.timingInfo.setTimeOfCurrentEvent(logEntry.getTime());
        this.currentEvent = Event.fromLogEntry(logEntry);
        this.pushEvent();
    }

    public void pushEof()
    {
        LOG.trace("Proceeding with EOF");
        this.currentEvent = Event.fromEof();
        this.pushEvent();
    }

    public boolean canProceed()
    {
        LOG.trace("Checking if automaton can proceed");

        if (this.thrownException != null)
        {
            LOG.trace("Cannot proceed because an exception was thrown");
            return false;
        }

        if (this.haltFlag)
        {
            LOG.trace("Cannot proceed because of halt flag");
            return false;
        }

        if (this.acceptFlag)
        {
            LOG.trace("Cannot proceed because of accept flag");
            return false;
        }

        if (this.rejectFlag)
        {
            LOG.trace("Cannot proceed because of reject flag");
            return false;
        }

        AutomatonNode checkNode;
        if (this.currentNode == null)
        {
            // Initialization complete but no log entry processed yet
            checkNode = this.initialNode;
        }
        else
        {
            checkNode = this.currentNode;
        }
        if (checkNode.getType() == Type.FAILURE)
        {
            LOG.trace("Cannot proceed because we're in a failure node");
            return false;
        }
        if (!checkNode.hasOutgoingEdges())
        {
            LOG.trace("Cannot proceed because there are no outgoing edges");
            return false;
        }
        return true;
    }

    public boolean succeeded()
    {
        if (this.automatonDefect())
        {
            LOG.debug("Run failed: Automaton is defect");
            return false;
        }
        if (this.acceptFlag)
        {
            LOG.debug("Run succeeded: Accept flag was set");
            return true;
        }
        if (this.rejectFlag)
        {
            LOG.debug("Run failed: Reject flag was set");
            return false;
        }
        AutomatonNode checkNode;
        if (this.currentNode == null)
        {
            // Initialization complete but no log entry processed yet
            checkNode = this.initialNode;
        }
        else
        {
            checkNode = this.currentNode;
        }
        final NodeSuccessState nodeState = this.getNodeSuccessState(checkNode);
        final boolean success = nodeState == NodeSuccessState.SUCCESS;
        if (success)
        {
            LOG.debug("Run succeeded: Node is succeeding");
        }
        else
        {
            LOG.debug("Run failed: Node is not succeeding but {}", nodeState);
        }
        return success;
    }

    public boolean automatonDefect()
    {
        return this.thrownException != null;
    }

    public String getErrorReason()
    {
        if (this.succeeded())
        {
            return null;
        }
        else
        {
            // Exceptions have priority
            if (this.thrownException != null)
            {
                return this.thrownException.getMessage();
            }
            else
            {
                if (this.rejectFlag)
                {
                    String msg = "A script called reject()";
                    if (StringUtils.isNotBlank(this.rejectMessage))
                    {
                        msg += ": " + this.rejectMessage;
                    }
                    return msg;
                }
                final AutomatonNode node = this.currentNode != null ? this.currentNode : this.initialNode;
                final NodeSuccessState failureReason = this.getNodeSuccessState(node);
                if (failureReason != NodeSuccessState.SUCCESS)
                {
                    return this.asssembleFinalNodeFailureMsg(node, failureReason);
                }
                else
                {
                    LOG.error("Automaton has not succeeded but there's no user readable error message why this happened");
                    return "Unknown error";
                }
            }
        }
    }

    public String getDescription()
    {
        return this.description;
    }

    private String asssembleFinalNodeFailureMsg(final AutomatonNode node, final NodeSuccessState failureReason)
    {
        String msg;
        if (failureReason == NodeSuccessState.NON_SUCCESS_NODE)
        {
            msg = "Final node '" + node + "' is not a succeeding state";
        }
        else if (failureReason == NodeSuccessState.SUCCESS_NODE_BUT_SUCCESS_CONDITION_FAILED)
        {
            msg = "Final node '" + node + "' is a succeeding state BUT success check expression gave 'false'";
        }
        else
        {
            throw new RuntimeException("Internal error - unhandled failure reason");
        }
        if (this.lastTransition != null)
        {
            msg += " (entered from node '" + this.lastTransition.getSourceNode() + "' via edge '"
                + this.lastTransition.getEdge() + "' at automaton time '" + this.lastTransition.getTimeMs() + "'";
            if (this.lastTransition.getEvent().isLogEntry())
            {
                msg += " with log line '" + this.lastTransition.getEvent().getLogEntry().getLineNumber() + "'";
            }
            else
            {
                msg += " with EOF";
            }
            msg += ")";
        }
        return msg;
    }

    private void pushEvent()
    {
        try
        {
            this.enterInitialNodeIfNotYetDone();

            if (!this.canProceed())
            {
                LOG.trace("Cannot proceed anymore");
                return;
            }

            assert this.currentNode != null;

            this.scriptEnvironment.updateEnvironment(this.currentEvent);

            final AutomatonNode eventStartNode = this.currentNode;

            this.visitedNodes.clear();
            while (this.proceed())
            {
                if (!this.canProceed())
                {
                    LOG.trace("Cannot proceed anymore");
                    break;
                }
                if (this.currentNode.getWait())
                {
                    LOG.trace("Wait flag set in current node; finishing transition");
                    break;
                }
            }

            final AutomatonNode eventEndNode = this.currentNode;

            if (eventStartNode != eventEndNode)
            {
                this.timingInfo.setTimeOfLastTransition(this.timingInfo.getTimeOfCurrentEvent());
            }

            LOG.debug("Microtransitions: {}", this.visitedNodes.size());
        }
        catch (final RuntimeException re)
        {
            LOG.trace("Got exception during execution", re);
            this.thrownException = re;
            throw re;
        }
    }

    private NodeSuccessState getNodeSuccessState(final AutomatonNode node)
    {
        if (node.getType() != Type.SUCCESS)
        {
            return NodeSuccessState.NON_SUCCESS_NODE;
        }
        else
        {
            if (node.getSuccessCheckExp().exists() && !node.getSuccessCheckExp().runAndGetBooleanResult())
            {
                LOG.debug("Cannot succeed with node '{}' as success check expression tells us 'no'", node);
                return NodeSuccessState.SUCCESS_NODE_BUT_SUCCESS_CONDITION_FAILED;
            }
            else
            {
                return NodeSuccessState.SUCCESS;
            }
        }
    }

    private void enterInitialNodeIfNotYetDone()
    {
        if (this.currentNode == null)
        {
            if (this.thrownException != null)
            {
                // Do not start processing when the automaton could not be initialized
                return;
            }
            this.onLoad.run();
            if (this.scriptEnvironment != null)
            {
                this.scriptEnvironment.updateEnvironment(this.currentEvent); // Load log entry after onLoad was executed
            }
            this.currentNode = this.initialNode;
            // Start with first timestamp, or 0, if there is none
            this.timingInfo.setStartTime(this.currentEvent.isLogEntry() ? this.currentEvent.getLogEntry().getTime() : 0L);
            this.timingInfo.setTimeOfLastMicrotransition(this.timingInfo.getStartTime());
            this.timingInfo.setTimeOfLastTransition(this.timingInfo.getStartTime());
            this.timingInfo.setTimeOfCurrentEvent(this.timingInfo.getStartTime());
            this.currentNode.getOnEnter().run();
        }
    }

    private void checkAutomatonAndFindInitialNode()
    {
        this.checkInitialNodeExistsAndFindIt();
        this.checkFailureNodesHaveNoOutgoingEdges();
        this.checkAllEdgesHaveAtLeastOneCondition();
        this.checkSuccessCheckExpIsOnlyAppliedToSuccessNodes();
    }

    private void checkSuccessCheckExpIsOnlyAppliedToSuccessNodes()
    {
        final List<AutomatonNode> nodesWithSuccessCheckExpButNotSuccessType = this.nodes.stream()
            .filter(node -> node.getSuccessCheckExp().exists() && node.getType() != Type.SUCCESS)
            .collect(Collectors.toList());
        if (nodesWithSuccessCheckExpButNotSuccessType.size() > 0)
        {
            throw new InvalidAutomatonDefinitionException("Nodes have success check expression, but are not of success type: "
                + nodesWithSuccessCheckExpButNotSuccessType.stream().map(edge -> edge.toString()).collect(Collectors.joining(", ")));
        }
    }

    private void checkAllEdgesHaveAtLeastOneCondition()
    {
        final List<AutomatonEdge> edgesWithoutCondition = this.edges.stream()
            .filter(edge -> !edge.hasActiveConditions())
            .collect(Collectors.toList());
        if (edgesWithoutCondition.size() > 0)
        {
            throw new InvalidAutomatonDefinitionException("Edges have no condition: "
                + edgesWithoutCondition.stream().map(edge -> edge.toString()).collect(Collectors.joining(", ")));
        }
    }

    private void checkFailureNodesHaveNoOutgoingEdges()
    {
        for (final AutomatonNode node : this.nodes)
        {
            if (node.getType() == Type.FAILURE && node.hasOutgoingEdges())
            {
                throw new InvalidAutomatonDefinitionException("A failure node must not have outgoing edges: " + node);
            }
        }
    }

    private void checkInitialNodeExistsAndFindIt()
    {
        final List<AutomatonNode> initialNodes = this.nodes.stream().filter(node -> node.getType() == Type.INITIAL)
            .collect(Collectors.toList());
        if (initialNodes.size() > 1)
        {
            throw new InvalidAutomatonDefinitionException(
                "Multiple initial nodes defined: " + initialNodes.stream().map(node -> node.toString()).collect(Collectors.joining(", ")));
        }
        if (initialNodes.size() == 0)
        {
            throw new InvalidAutomatonDefinitionException("No initial node defined.");
        }
        this.initialNode = initialNodes.get(0);
    }

    private void loadAutomatonFromDefinition()
    {
        LOG.debug("Loading automaton from definition");
        try
        {
            this.automatonDefinition.load();
        }
        catch (final AutomatonLoadingException ale)
        {
            throw new InvalidAutomatonDefinitionException("Failed to load automaton!", ale);
        }

        Validate.isTrue(this.scriptingEngine == null); // Must not be initialized yet; otherwise, the setting has no more effect
        this.scriptLanguage = this.automatonDefinition.getScriptLanguage();
        this.initScriptEngine();
        final List<? extends INodeDefinition> externalNodes = this.automatonDefinition.getNodes();
        final List<? extends IEdgeDefinition> externalEdges = this.automatonDefinition.getEdges();
        this.onLoad = new EmbeddedScript(this.automatonDefinition.getOnLoad());
        this.description = this.automatonDefinition.getDescription();

        final Map<INodeDefinition, AutomatonNode> mapNodeDefinitionsToInternalNode = new HashMap<>();
        this.loadNodesFromDefinition(externalNodes, mapNodeDefinitionsToInternalNode);
        this.loadEdgesFromDefinition(externalEdges, mapNodeDefinitionsToInternalNode);
        LOG.debug("Loaded '{}' nodes and '{}' edges", this.nodes.size(), this.edges.size());
    }

    private void loadEdgesFromDefinition(final List<? extends IEdgeDefinition> externalEdges,
        final Map<INodeDefinition, AutomatonNode> mapNodeDefinitionsToInternalNode)
    {
        LOG.debug("Loading edges from definition");
        for (final IEdgeDefinition edgeDefinition : externalEdges)
        {
            final INodeDefinition src = edgeDefinition.getSource();
            final AutomatonNode srcNode = mapNodeDefinitionsToInternalNode.get(src);
            if (srcNode == null)
            {
                throw new InvalidAutomatonDefinitionException("Source node '" + src + "' of edge '" + edgeDefinition + "' not found!");
            }
            final INodeDefinition dest = edgeDefinition.getDestination();
            final AutomatonNode destNode = mapNodeDefinitionsToInternalNode.get(dest);
            if (destNode == null)
            {
                throw new InvalidAutomatonDefinitionException(
                    "Destination node '" + dest + "' of edge '" + edgeDefinition + "' not found!");
            }

            final AutomatonEdge newEdge = new AutomatonEdge(edgeDefinition);
            newEdge.setSource(srcNode);
            newEdge.setDestination(destNode);
            newEdge.setOnWalk(new EmbeddedScript(edgeDefinition.getOnWalk()));
            if (edgeDefinition.getChannel() == IEdgeDefinition.DEFAULT_CHANNEL)
            {
                newEdge.setChannel(ILogEntry.DEFAULT_CHANNEL);
            }
            else
            {
                newEdge.setChannel(edgeDefinition.getChannel());
            }
            IEdgeDefinition.RequiredConditions requiredConditionsSetting = edgeDefinition.getRequiredConditions();
            if (requiredConditionsSetting == null)
            {
                requiredConditionsSetting = IEdgeDefinition.DEFAULT_REQUIRED_CONDITIONS_VALUE;
            }
            if (requiredConditionsSetting == IEdgeDefinition.RequiredConditions.ALL)
            {
                newEdge.setRequiredConditionsSetting(AutomatonEdge.RequiredConditions.ALL);
            }
            else if (requiredConditionsSetting == IEdgeDefinition.RequiredConditions.ONE)
            {
                newEdge.setRequiredConditionsSetting(AutomatonEdge.RequiredConditions.ONE);
            }
            newEdge.initConditions(this.parameters, this.scriptingEngine);
            this.edges.add(newEdge);
            srcNode.addOutgoingEdge(newEdge);
            destNode.addIncomingEdge(newEdge);
        }
        LOG.debug("Edges from definition loaded");
    }

    private void loadNodesFromDefinition(final List<? extends INodeDefinition> externalNodes,
        final Map<INodeDefinition, AutomatonNode> mapNodeDefinitionsToInternalNode)
    {
        LOG.debug("Loading nodes from definition");
        for (final INodeDefinition nodeDefinition : externalNodes)
        {
            final AutomatonNode newNode = new AutomatonNode(nodeDefinition);
            final String externalNodeName = nodeDefinition.toString();
            newNode.setName(this.parameters
                .insertAllParameters(externalNodeName == null ? null : this.parameters.insertAllParameters(externalNodeName)));
            if (nodeDefinition.getType() == INodeDefinition.Type.INITIAL)
            {
                newNode.setType(AutomatonNode.Type.INITIAL);
            }
            if (nodeDefinition.getType() == INodeDefinition.Type.FAILURE)
            {
                newNode.setType(AutomatonNode.Type.FAILURE);
            }
            if (nodeDefinition.getType() == INodeDefinition.Type.SUCCESS)
            {
                newNode.setType(AutomatonNode.Type.SUCCESS);
            }
            newNode.setOnEnter(new EmbeddedScript(nodeDefinition.getOnEnter()));
            newNode.setOnLeave(new EmbeddedScript(nodeDefinition.getOnLeave()));
            newNode.setSuccessCheckExp(new EmbeddedScript(nodeDefinition.getSuccessCheckExp()));
            newNode.setWait(nodeDefinition.getWait());
            this.nodes.add(newNode);
            mapNodeDefinitionsToInternalNode.put(nodeDefinition, newNode);
        }
        LOG.debug("Nodes from definition loaded");
    }

    private boolean proceed()
    {
        LOG.trace("Proceeding to next node");
        final List<AutomatonEdge> matchingEdges = new ArrayList<>(this.currentNode.getOutgoingEdges().size());
        for (final AutomatonEdge edge : this.currentNode.getOutgoingEdges())
        {
            if (edge.edgeMatchesEvent(new EventForConditionEvaluation(), this.timingInfo))
            {
                matchingEdges.add(edge);
                LOG.trace("Found matching edge: '{}'", edge);
            }
        }

        if (matchingEdges.isEmpty())
        {
            LOG.trace("No matching edges");
            return false;
        }

        if (matchingEdges.size() > 1)
        {
            if (!this.checkMatchingEdgesAreEquivalent(matchingEdges))
            {
                throw new ExecutionException("Found ambiguous nodes to proceed to from node '" + this.currentNode
                    + "' as more than one edge is matching: "
                    + matchingEdges.stream().map(edge -> edge.toString()).collect(Collectors.joining(", ")));
            }
        }
        final AutomatonEdge matchingEdge = matchingEdges.get(0);

        final boolean closingALoop = this.haveLoop(matchingEdge);
        final AutomatonNode lastNode = this.currentNode;
        this.proceedViaEdge(matchingEdge);
        if (this.currentEvent.isLogEntry())
        {
            LOG.debug("Proceeded to next state: Node '{}' -> '{}' via edge '{}' triggered by log entity on line '{}'",
                lastNode, this.currentNode, matchingEdge, this.currentEvent.getLogEntry().getLineNumber());
        }
        else
        {
            LOG.debug("Proceeded to next state: Node '{}' -> '{}' via edge '{}' triggered by EOF",
                lastNode, this.currentNode, matchingEdge);
        }
        if (closingALoop)
        {
            LOG.trace("Closed loop. Stopping.");
            return false;
        }
        return true;
    }

    private boolean checkMatchingEdgesAreEquivalent(final List<AutomatonEdge> matchingEdges)
    {
        LOG.trace("Checking if '{}' edges point to different nodes or have scripts", matchingEdges.size());

        // Edges are not equivalent if they lead to different nodes
        AutomatonNode dest = null;
        for (final AutomatonEdge edge : matchingEdges)
        {
            if (dest == null)
            {
                dest = edge.getDestinationNode();
            }
            else
            {
                if (dest != edge.getDestinationNode())
                {
                    LOG.trace("Edges are not equivalent as some of them point to different targets ('{}' and '{}')", dest,
                        edge.getDestinationNode());
                    return false;
                }
            }
        }

        // Edges are not equivalent if they contain scripting actions
        if (matchingEdges.stream().anyMatch(edge -> edge.getOnWalk().exists()))
        {
            LOG.trace("Edges are not equivalent as of scripting actions having potential side effects");
            return false;
        }

        return true;
    }

    private boolean haveLoop(final AutomatonEdge matchingEdge)
    {
        this.visitedNodes.add(matchingEdge.getSourceNode());
        if (this.visitedNodes.contains(matchingEdge.getDestinationNode()))
        {
            // As this can be intended (self loops on nodes), only log this on trace level as it could spam too much output.
            if (LOG.isTraceEnabled())
            {
                this.visitedNodes.push(this.visitedNodes.getFirst()); // Close loop to make the following exception message more intuitive
                LOG.trace("Found loop during execution: {}, stopping at transaction base node",
                    this.visitedNodes.stream().map(node -> node.toString()).collect(Collectors.joining(" -> ")));
                this.visitedNodes.pop();
            }
            return true;
        }
        return false;
    }

    private void proceedViaEdge(final AutomatonEdge edge)
    {
        assert this.currentNode != null;
        this.currentNode.getOnLeave().run();
        this.currentNode = edge.getDestinationNode();
        if (edge.getOnWalk().exists())
        {
            edge.beforeOnWalk(this.scriptEnvironment);
            edge.getOnWalk().run();
            edge.afterOnWalk(this.scriptEnvironment);
        }
        this.currentNode.getOnEnter().run();
        this.lastTransition = new LastTransition(edge, this.currentEvent, this.timingInfo.getTimeOfCurrentEvent());
        this.timingInfo.setTimeOfLastMicrotransition(this.timingInfo.getTimeOfCurrentEvent());
    }

    private void initScriptEngine()
    {
        final ScriptEngineManager manager = new ScriptEngineManager();
        final String language = StringUtils.defaultIfBlank(this.scriptLanguage, this.DEFAULT_SCRIPTING_LANGUAGE);
        this.scriptingEngine = manager.getEngineByName(language);
        if (this.scriptingEngine == null)
        {
            final String msg = "Scripting engine for language '" + language + "' could not be initalized";
            LOG.error(msg);
            throw new ExecutionException(msg);
        }
        else
        {
            LOG.debug("Using script language: {}", language);
        }
        this.scriptEnvironment = new ScriptEnvironment();
        this.scriptingEngine.put("engine", this.scriptEnvironment);
    }

    private void compileScripts()
    {
        this.onLoad.compile(this.scriptingEngine);

        this.nodes.stream().forEach(node -> {
            node.getOnEnter().compile(this.scriptingEngine);
            node.getOnLeave().compile(this.scriptingEngine);
            node.getSuccessCheckExp().compile(this.scriptingEngine);
        });

        this.edges.stream().forEach(edge -> {
            edge.getOnWalk().compile(this.scriptingEngine);
        });
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return this.automatonDefinition.toString();
    }
}
