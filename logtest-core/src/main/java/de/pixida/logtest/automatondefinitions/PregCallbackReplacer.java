/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PregCallbackReplacer
{
    private final Pattern pattern;

    public PregCallbackReplacer(final String regex)
    {
        this.pattern = Pattern.compile(regex);
    }

    public String replaceMatches(final String input, final Function<Matcher, String> callback)
    {
        final StringBuffer resultString = new StringBuffer();
        final Matcher regexMatcher = this.pattern.matcher(input);
        int offset = 0;
        while (regexMatcher.find())
        {
            resultString.append(input.substring(offset, regexMatcher.start()));
            resultString.append(callback.apply(regexMatcher));
            offset = regexMatcher.end();
        }
        resultString.append(input.substring(offset));
        return resultString.toString();
    }
}
