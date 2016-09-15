/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EmbeddedScriptTest
{
    private ScriptEngine scriptingEngine;

    public EmbeddedScriptTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Before
    public void initScriptingEngine()
    {
        this.scriptingEngine = new ScriptEngineManager().getEngineByName("JavaScript");
        Assert.assertNotNull(this.scriptingEngine);
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionIsThrownWhenScriptHasNotBeenCompiled()
    {
        new EmbeddedScript("true").run();
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionIsThrownWhenScriptHasCompilationErrors()
    {
        new EmbeddedScript("a++++++").compile(this.scriptingEngine);
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionIsThrownWhenScriptHasErrors()
    {
        this.compile("y = x / 0").run();
    }

    @Test
    public void testHashObjectsAreEvaluatedToFalse()
    {
        Assert.assertFalse(this.compile("var obj = {}; obj").runAndGetBooleanResult());
    }

    @Test
    public void testArraysAreEvaluatedToFalse()
    {
        Assert.assertFalse(this.compile("var obj = []; obj").runAndGetBooleanResult());
    }

    @Test
    public void testTrueEvaluatesToTrue()
    {
        Assert.assertTrue(this.compile("true").runAndGetBooleanResult());
    }

    @Test
    public void testFalseEvaluatesToFalse()
    {
        Assert.assertFalse(this.compile("false").runAndGetBooleanResult());
    }

    @Test
    public void testZeroIntEvaluatesToFalse()
    {
        Assert.assertFalse(this.compile("0").runAndGetBooleanResult());
    }

    @Test
    public void testNonZeroIntEvaluatesToTrue()
    {
        Assert.assertTrue(this.compile("5").runAndGetBooleanResult());
    }

    @Test
    public void testEmptyStringEvaluatesToFalse()
    {
        Assert.assertFalse(this.compile("''").runAndGetBooleanResult());
    }

    @Test
    public void testNonEmptyStringEvaluatesToTrue()
    {
        Assert.assertTrue(this.compile("'xx'").runAndGetBooleanResult());
    }

    @Test
    public void testZeroFloatOrDoubleEvaluatesToFalse()
    {
        Assert.assertFalse(this.compile("0.0").runAndGetBooleanResult());
    }

    @Test
    public void testNonZeroFloatOrDoubleEvaluatesToTrue()
    {
        Assert.assertTrue(this.compile("123.5").runAndGetBooleanResult());
    }

    @Test
    public void testNullEvaluatesToFalse()
    {
        Assert.assertFalse(this.compile("null").runAndGetBooleanResult());
    }

    @Test
    public void testNullScriptIsNotEvaluated()
    {
        Assert.assertFalse(this.compile(null).exists());
    }

    @Test
    public void testEmptyScriptIsNotEvaluated()
    {
        Assert.assertFalse(this.compile("").exists());
    }

    @Test
    public void testWhitespacedScriptIsNotEvaluated()
    {
        Assert.assertFalse(this.compile("     ").exists());
    }

    private EmbeddedScript compile(final String script)
    {
        final EmbeddedScript s = new EmbeddedScript(script);
        s.compile(this.scriptingEngine);
        return s;
    }
}
