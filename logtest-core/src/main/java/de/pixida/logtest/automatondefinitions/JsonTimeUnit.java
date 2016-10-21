/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public enum JsonTimeUnit
{
    NS("ns", TimeUnit.NANOSECONDS),
    MYCS("µs", TimeUnit.MICROSECONDS),
    MS("ms", TimeUnit.MILLISECONDS),
    S("s", TimeUnit.SECONDS),
    M("m", TimeUnit.MINUTES),
    H("h", TimeUnit.HOURS),
    D("d", TimeUnit.DAYS);

    private String name;
    private TimeUnit timeUnit;

    private JsonTimeUnit(final String aName, final TimeUnit aTimeUnit)
    {
        this.name = aName;
        this.timeUnit = aTimeUnit;
    }

    String getName()
    {
        return this.name;
    }

    TimeUnit getTimeUnit()
    {
        return this.timeUnit;
    }

    static public String convertTimeUnitToString(final TimeUnit value)
    {
        // This is O(# defined constants in this enumeration); should be acceptable for now.
        for (final JsonTimeUnit v : values())
        {
            if (v.getTimeUnit() == value)
            {
                return v.getName();
            }
        }
        return null;
    }

    static public List<String> getListOfPossibleNames()
    {
        return Arrays.stream(values()).map(entry -> entry.getName()).collect(Collectors.toList());
    }

    static public TimeUnit indexToTimeUnit(final int value)
    {
        return values()[value].getTimeUnit();
    }
}
