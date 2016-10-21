/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import org.apache.commons.lang3.StringUtils;

public class GenericNode implements INodeDefinition
{
    private final String id;
    private String name;
    private Type type;
    private String onEnter;
    private String onLeave;
    private String successCheckExp;
    private boolean wait;
    private String description;

    private String nameForLogging;

    public GenericNode(final String aId)
    {
        this.id = aId;
        this.createNameForLogging();
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public Type getType()
    {
        return this.type;
    }

    public void setType(final Type value)
    {
        this.type = value;
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
    public String getName()
    {
        return this.name;
    }

    public void setName(final String value)
    {
        this.name = value;
        this.createNameForLogging();
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(final String value)
    {
        this.description = value;
    }

    private void createNameForLogging()
    {
        if (StringUtils.isNotBlank(this.name))
        {
            this.nameForLogging = this.replaceWhitespacesAgainstSpace(this.name);
        }
        else if (StringUtils.isNotBlank(this.id))
        {
            this.nameForLogging = this.replaceWhitespacesAgainstSpace(this.id);
        }
        else
        {
            this.nameForLogging = "";
        }
    }

    private String replaceWhitespacesAgainstSpace(final String value)
    {
        return value.replaceAll("\\s", " ");
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return this.nameForLogging;
    }
}
