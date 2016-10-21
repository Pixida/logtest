/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import org.apache.commons.lang3.Validate;

import javafx.scene.Node;

class GraphShape
{
    private final Node node;
    private final int zIndex;

    GraphShape(final Node aNode, final int aZIndex)
    {
        Validate.notNull(aNode);
        this.node = aNode;
        this.zIndex = aZIndex;
    }

    Node getNode()
    {
        return this.node;
    }

    int getZIndex()
    {
        return this.zIndex;
    }

    @Override
    public int hashCode()
    {
        return this.node.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }
        if (obj.getClass() != this.getClass())
        {
            return false;
        }
        final GraphShape other = (GraphShape) obj;
        return this.node.equals(other.node);
    }
}
