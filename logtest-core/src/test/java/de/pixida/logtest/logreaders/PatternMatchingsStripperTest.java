/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.logreaders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class PatternMatchingsStripperTest
{
    public PatternMatchingsStripperTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testStrippingOneMatch()
    {
        Assert.assertEquals("ac", this.strip("a(b)c", "abc"));
    }

    @Test
    public void testStrippingMultipleMatches()
    {
        Assert.assertEquals("ac", this.strip("a(b)c(d)", "abcd"));
    }

    @Test
    public void testStrippingNestedMatches()
    {
        Assert.assertEquals("ae", this.strip("a((b)cd)e", "abcde"));
        Assert.assertEquals("ae", this.strip("a(b(c)d)e", "abcde"));
        Assert.assertEquals("ae", this.strip("a(bc(d))e", "abcde"));
        Assert.assertEquals("ae", this.strip("a((bc)d)e", "abcde"));
        Assert.assertEquals("ae", this.strip("a(b(cd))e", "abcde"));
        Assert.assertEquals("ae", this.strip("a((bcd))e", "abcde"));
    }

    @Test
    public void testStringIsClearedWhenMatchCoversWholeString()
    {
        Assert.assertEquals("", this.strip("(abc)", "abc"));
    }

    @Test
    public void testNothingIsStrippedWhenThereIsNoMatch()
    {
        Assert.assertEquals("abc", this.strip("abc", "abc"));
    }

    @Test
    public void testNothingIsStrippedWhenOptionalMatchIsEmpty()
    {
        Assert.assertEquals("ac", this.strip("a(b)?c", "ac"));
    }

    private String strip(final String regex, final String value)
    {
        final Matcher matcher = Pattern.compile(regex).matcher(value);
        Assert.assertTrue(matcher.find());
        return PatternMatchingsStripper.strip(matcher, value);
    }
}
