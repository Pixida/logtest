/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import de.pixida.logtest.automatondefinitions.INodeDefinition;

class AutomatonNode
{
    /*
     * Why can a node only have one of those types?
     *
     * INITIAL + SUCCESS: Would create automatons which succeed even if no log message is processed. This behavior is usually designed by
     * accident. If the behavior is desired, it can explicitly be defined. [INITIAL] --EOF--> [SUCCESS].
     *
     * INITIAL + FAILURE: Obviously contradictory.
     *
     * INITIAL + FAILURE: A failure node cannot have outgoing edges. This automaton would always fail.
     */
    enum Type
    {
        INITIAL,
        SUCCESS,
        FAILURE
    }

    private Type type;
    private final List<AutomatonEdge> outgoingEdges = new ArrayList<>();
    private final List<AutomatonEdge> incomingEdges = new ArrayList<>();
    private EmbeddedScript onEnter;
    private EmbeddedScript onLeave;
    private EmbeddedScript successCheckExp;
    private String name;
    private boolean wait;

    AutomatonNode(final INodeDefinition node)
    {
        Validate.notNull(node);
    }

    void setName(final String value)
    {
        this.name = value;
    }

    Type getType()
    {
        return this.type;
    }

    void setType(final Type value)
    {
        this.type = value;
    }

    void addOutgoingEdge(final AutomatonEdge value)
    {
        this.outgoingEdges.add(value);
    }

    void addIncomingEdge(final AutomatonEdge value)
    {
        this.incomingEdges.add(value);
    }

    boolean hasOutgoingEdges()
    {
        return this.outgoingEdges.size() > 0;
    }

    List<AutomatonEdge> getOutgoingEdges()
    {
        return this.outgoingEdges;
    }

    EmbeddedScript getOnEnter()
    {
        return this.onEnter;
    }

    EmbeddedScript getOnLeave()
    {
        return this.onLeave;
    }

    void setOnEnter(final EmbeddedScript value)
    {
        this.onEnter = value;
    }

    void setOnLeave(final EmbeddedScript value)
    {
        this.onLeave = value;
    }

    EmbeddedScript getSuccessCheckExp()
    {
        return this.successCheckExp;
    }

    void setSuccessCheckExp(final EmbeddedScript value)
    {
        this.successCheckExp = value;
    }

    boolean getWait()
    {
        return this.wait;
    }

    void setWait(final boolean value)
    {
        this.wait = value;
    }

    // Just for logging output / used for debugging
    @Override
    public String toString()
    {
        return this.name;
    }
}
