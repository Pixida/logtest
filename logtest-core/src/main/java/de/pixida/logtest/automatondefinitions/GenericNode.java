/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.Set;

public class GenericNode implements INodeDefinition
{
    private final String id;
    private Set<Flag> flags;
    private String onEnter;
    private String onLeave;
    private String successCheckExp;
    private boolean wait;
    private String comment;

    public GenericNode(final String aId, final Set<Flag> aFlags)
    {
        this.id = aId;
        this.flags = aFlags;
    }

    @Override
    public Set<Flag> getFlags()
    {
        return this.flags;
    }

    public void setFlags(final Set<Flag> value)
    {
        this.flags = value;
    }

    @Override
    public String getOnEnter()
    {
        return this.onEnter;
    }

    @Override
    public String getOnLeave()
    {
        return this.onLeave;
    }

    @Override
    public String getSuccessCheckExp()
    {
        return this.successCheckExp;
    }

    @Override
    public boolean getWait()
    {
        return this.wait;
    }

    public void setOnEnter(final String value)
    {
        this.onEnter = value;
    }

    public void setOnLeave(final String value)
    {
        this.onLeave = value;
    }

    public void setSuccessCheckExp(final String value)
    {
        this.successCheckExp = value;
    }

    public void setWait(final boolean value)
    {
        this.wait = value;
    }

    @Override
    public String getComment()
    {
        return this.comment;
    }

    public void setComment(final String value)
    {
        this.comment = value;
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return this.id;
    }
}
