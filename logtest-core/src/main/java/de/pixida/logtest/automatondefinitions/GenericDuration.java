/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.concurrent.TimeUnit;

public class GenericDuration implements IDuration
{
    private String value;
    private TimeUnit unit;
    private boolean isInclusive;

    public GenericDuration()
    {
        // Empty constructor needed by checkstyle
    }

    @Override
    public String getValue()
    {
        return this.value;
    }

    public void setValue(final String val)
    {
        this.value = val;
    }

    @Override
    public TimeUnit getUnit()
    {
        return this.unit;
    }

    public void setUnit(final TimeUnit val)
    {
        this.unit = val;
    }

    @Override
    public boolean isInclusive()
    {
        return this.isInclusive;
    }

    public void setInclusive(final boolean val)
    {
        this.isInclusive = val;
    }
}
