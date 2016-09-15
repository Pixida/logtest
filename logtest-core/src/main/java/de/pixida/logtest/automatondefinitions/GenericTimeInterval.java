/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

public class GenericTimeInterval implements ITimeInterval
{
    private IDuration min;
    private IDuration max;

    public GenericTimeInterval()
    {
        // Empty constructor needed by checkstyle
    }

    @Override
    public IDuration getMin()
    {
        return this.min;
    }

    public void setMin(final IDuration value)
    {
        this.min = value;
    }

    @Override
    public IDuration getMax()
    {
        return this.max;
    }

    public void setMax(final IDuration value)
    {
        this.max = value;
    }
}
