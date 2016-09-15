/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine.conditions;

abstract class BaseCondition implements ICondition
{
    private boolean isActive;

    public BaseCondition()
    {
        // Empty constructor needed by checkstyle
    }

    @Override
    public boolean isActive()
    {
        return this.isActive;
    }

    @Override
    public void beforeOnWalk(final IScriptEnvironment scriptEnvironment)
    {
        // By default, do nothing
    }

    @Override
    public void afterOnWalk(final IScriptEnvironment scriptEnvironment)
    {
        // By default, do nothing
    }

    protected void setIsActive(final boolean value)
    {
        this.isActive = value;
    }
}
