/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;

public class ScriptingEngineTest
{
    public ScriptingEngineTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testItsPossibleToCopyPrimitiveValuesBetweenTwoEngines() throws ScriptException
    {
        final ScriptEngineManager manager = new ScriptEngineManager();

        final ScriptEngine engine1 = manager.getEngineByName("JavaScript");
        final String intValName = "intVal";
        final int intVal = 5;
        engine1.eval(intValName + " = " + intVal + ";");
        Assert.assertTrue(engine1.getBindings(ScriptContext.ENGINE_SCOPE).get(intValName) instanceof Integer);
        Assert.assertEquals(intVal, engine1.getBindings(ScriptContext.ENGINE_SCOPE).get(intValName));

        final ScriptEngine engine2 = manager.getEngineByName("JavaScript");
        engine2.getBindings(ScriptContext.ENGINE_SCOPE).put(intValName, intVal);
        Assert.assertTrue(engine2.getBindings(ScriptContext.ENGINE_SCOPE).get(intValName) instanceof Integer);
        Assert.assertEquals(intVal, engine2.getBindings(ScriptContext.ENGINE_SCOPE).get(intValName));
    }
}
