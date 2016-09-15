/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.logreaders;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.tuple.Pair;

public abstract class PatternMatchingsStripper
{
    public static String strip(final Matcher matcher, final String value)
    {
        if (matcher.groupCount() == 0)
        {
            return value;
        }

        // Remove optional matches which were empty and filter nested matches
        final List<Pair<Integer, Integer>> realMatches = new ArrayList<>();
        int lastMostOuterMatchEnd = -1;
        for (int i = 1; i <= matcher.groupCount(); i++)
        {
            if (matcher.start(i) != -1)
            {
                if (matcher.end(i) <= lastMostOuterMatchEnd)
                {
                    continue;
                }
                lastMostOuterMatchEnd = matcher.end(i);
                realMatches.add(Pair.of(matcher.start(i), matcher.end(i)));
            }
        }
        if (realMatches.isEmpty())
        {
            return value;
        }

        // Removal
        final StringBuilder sb = new StringBuilder(value.substring(0, realMatches.get(0).getLeft()));
        for (int i = 0; i < realMatches.size(); i++)
        {
            // Assumption: Substring before start of match was already appended
            if (i + 1 < realMatches.size())
            {
                sb.append(value.substring(realMatches.get(i).getRight(), realMatches.get(i + 1).getLeft()));
            }
            else
            {
                sb.append(value.substring(realMatches.get(i).getRight()));
            }
        }
        return sb.toString();
    }
}
