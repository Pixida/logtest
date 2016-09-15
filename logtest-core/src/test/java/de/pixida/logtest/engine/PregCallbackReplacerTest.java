/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import org.junit.Assert;
import org.junit.Test;

import de.pixida.logtest.automatondefinitions.PregCallbackReplacer;

public class PregCallbackReplacerTest
{
    public PregCallbackReplacerTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testThatReplacementWorksCorrectly()
    {
        Assert.assertEquals("Hello Alice, this is Bob!",
            new PregCallbackReplacer("\\$\\{(.+?)\\}").replaceMatches("Hello ${name}, this is ${otherName}!",
                match -> {
                    if (match.group(1).equals("name"))
                    {
                        return "Alice";
                    }
                    if (match.group(1).equals("otherName"))
                    {
                        return "Bob";
                    }
                    throw new RuntimeException();
                }));
    }

    @Test
    public void testThatReplacementsAreNotReplacedAgain()
    {
        Assert.assertEquals("${b}",
            new PregCallbackReplacer("\\$\\{(.*?)\\}").replaceMatches("${a}",
                match -> {
                    if (match.group(1).equals("a"))
                    {
                        return "${b}";
                    }
                    throw new RuntimeException();
                }));
    }
}
