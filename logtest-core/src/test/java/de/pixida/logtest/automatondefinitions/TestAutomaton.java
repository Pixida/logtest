/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestAutomaton implements IAutomatonDefinition
{
    private final List<GenericNode> genericNodes = new ArrayList<>();
    private final List<GenericEdge> genericEdges = new ArrayList<>();

    private int runningNodeId = 0;
    private int runningEdgeId = 0;
    private String onLoad;
    private String description;
    private String scriptLanguage;

    public static class GenericNodeBuilder
    {
        private final GenericNode node;

        GenericNodeBuilder(final GenericNode aNode)
        {
            this.node = aNode;
        }

        public GenericNodeBuilder withType(final INodeDefinition.Type value)
        {
            this.node.setType(value);
            return this;
        }

        public GenericNodeBuilder withOnEnter(final String value)
        {
            this.node.setOnEnter(value);
            return this;
        }

        public GenericNodeBuilder withOnLeave(final String value)
        {
            this.node.setOnLeave(value);
            return this;
        }

        public GenericNodeBuilder withSuccessCheckExp(final String value)
        {
            this.node.setSuccessCheckExp(value);
            return this;
        }

        public GenericNodeBuilder withWait()
        {
            this.node.setWait(true);
            return this;
        }

        public GenericNode get()
        {
            return this.node;
        }
    }

    public static class GenericEdgeBuilder
    {
        private final GenericEdge edge;

        GenericEdgeBuilder(final GenericEdge aEdge)
        {
            this.edge = aEdge;
        }

        public GenericEdgeBuilder withRegExp(final String value)
        {
            this.edge.setRegExp(value);
            return this;
        }

        public GenericEdgeBuilder withCheckExp(final String value)
        {
            this.edge.setCheckExp(value);
            return this;
        }

        public GenericEdgeBuilder withTriggerAlways()
        {
            this.edge.setTriggerAlways(true);
            return this;
        }

        public GenericEdgeBuilder withOnWalk(final String value)
        {
            this.edge.setOnWalk(value);
            return this;
        }

        public GenericEdgeBuilder withEofCondition()
        {
            this.edge.setTriggerOnEof(true);
            return this;
        }

        public GenericEdgeBuilder withChannel(final String value)
        {
            this.edge.setChannel(value);
            return this;
        }

        public GenericEdgeBuilder withRequiredConditions(final IEdgeDefinition.RequiredConditions value)
        {
            this.edge.setRequiredConditions(value);
            return this;
        }

        public GenericEdgeBuilder withTimeIntervalSinceLastMicrotransitionMin(final String value, final TimeUnit unit,
            final boolean isInclusive)
        {
            assert this.obtainTimeIntervalSinceLastMicrotransition() instanceof GenericTimeInterval;
            ((GenericTimeInterval) this.obtainTimeIntervalSinceLastMicrotransition()).setMin(this.createDuration(value, unit, isInclusive));
            return this;
        }

        public GenericEdgeBuilder withTimeIntervalSinceLastMicrotransitionMax(final String value, final TimeUnit unit,
            final boolean isInclusive)
        {
            ((GenericTimeInterval) this.obtainTimeIntervalSinceLastMicrotransition()).setMax(this.createDuration(value, unit, isInclusive));
            return this;
        }

        private ITimeInterval obtainTimeIntervalSinceLastMicrotransition()
        {
            ITimeInterval ti = this.edge.getTimeIntervalSinceLastMicrotransition();
            if (ti == null)
            {
                ti = new GenericTimeInterval();
                this.edge.setTimeIntervalSinceLastMicrotransition(ti);
            }
            return ti;
        }

        public GenericEdgeBuilder withTimeIntervalSinceLastMicrotransition(final String value)
        {
            this.edge.setTimeIntervalSinceLastMicrotransition(StringToGenericTimeIntervalConverter.convertStringToTimestamp(value));
            return this;
        }

        public GenericEdgeBuilder withTimeIntervalSinceLastMicrotransition(final long value)
        {
            return this.withTimeIntervalSinceLastMicrotransition(String.valueOf(value));
        }

        public GenericEdgeBuilder withTimeIntervalSinceLastTransition(final String value)
        {
            this.edge.setTimeIntervalSinceLastTransition(StringToGenericTimeIntervalConverter.convertStringToTimestamp(value));
            return this;
        }

        public GenericEdgeBuilder withTimeIntervalSinceAutomatonStart(final String value)
        {
            this.edge.setTimeIntervalSinceAutomatonStart(StringToGenericTimeIntervalConverter.convertStringToTimestamp(value));
            return this;
        }

        public GenericEdgeBuilder withTimeIntervalForEvent(final String value)
        {
            this.edge.setTimeIntervalForEvent(StringToGenericTimeIntervalConverter.convertStringToTimestamp(value));
            return this;
        }

        public GenericEdge get()
        {
            return this.edge;
        }

        private IDuration createDuration(final String value, final TimeUnit unit, final boolean isInclusive)
        {
            final GenericDuration d = new GenericDuration();
            d.setValue(value);
            d.setUnit(unit);
            d.setInclusive(isInclusive);
            return d;
        }
    }

    public TestAutomaton()
    {
        // Empty constructor needed by checkstyle
    }

    public TestAutomaton withDescription(final String value)
    {
        this.description = value;
        return this;
    }

    public TestAutomaton withScriptLanguage(final String value)
    {
        this.scriptLanguage = value;
        return this;
    }

    public TestAutomaton withOnLoad(final String value)
    {
        this.onLoad = value;
        return this;
    }

    public GenericNodeBuilder createNode()
    {
        return this.createNode(null);
    }

    public GenericNodeBuilder createNode(final String id)
    {
        final GenericNode newNode = new GenericNode(id == null ? String.valueOf(this.runningNodeId++) : id);
        this.genericNodes.add(newNode);
        final GenericNodeBuilder builder = new GenericNodeBuilder(newNode);
        return builder;
    }

    public GenericEdgeBuilder createEdge(final GenericNode source, final GenericNode destination)
    {
        final GenericEdge newEdge = new GenericEdge(String.valueOf(this.runningEdgeId++), source, destination);
        this.genericEdges.add(newEdge);
        final GenericEdgeBuilder builder = new GenericEdgeBuilder(newEdge);
        return builder;
    }

    @Override
    public List<INodeDefinition> getNodes()
    {
        return new ArrayList<>(this.genericNodes);
    }

    @Override
    public List<IEdgeDefinition> getEdges()
    {
        return new ArrayList<>(this.genericEdges);
    }

    @Override
    public String getOnLoad()
    {
        return this.onLoad;
    }

    @Override
    public String getScriptLanguage()
    {
        return this.scriptLanguage;
    }

    @Override
    public String toString()
    {
        return "Test automaton";
    }

    @Override
    public String getDisplayName()
    {
        return "Test automaton name";
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public void load()
    {
        // Nothing to do, we're always "loaded"
    }
}
