/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedScript
{
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedScript.class);

    private final String script;
    private final boolean exists;
    private CompiledScript compiledScript;

    public EmbeddedScript(final String aScript)
    {
        this.script = aScript;
        this.exists = !StringUtils.isBlank(this.script);
    }

    public boolean exists()
    {
        return this.exists;
    }

    public void compile(final ScriptEngine scriptingEngine)
    {
        if (!this.exists())
        {
            LOG.trace("Not compiling empty script");
            return;
        }
        if (!(scriptingEngine instanceof Compilable))
        {
            // We compile scripts to improve performance. The code could be rewritten to support engines which are not capable of
            // precompiling, but for now, the current engine does, so there's no reason to handle this, as it can be easily found out
            // during unit testing. Therefore, we should never get here under real conditions.
            throw new RuntimeException("Scripting engine does not support precompiling scripts");
        }

        LOG.trace("Compiling script: {}", this.script);
        try
        {
            this.compiledScript = ((Compilable) scriptingEngine).compile(this.script);
        }
        catch (final ScriptException se)
        {
            // The error message draws ascii art, so it is important it starts on a new line. E.g.:
            // Line 5: for [
            //             ^ Expected '('
            throw new InvalidAutomatonDefinitionException("Script compilation error:\n" + se.getMessage());
        }
        LOG.trace("Script compilation finished");
    }

    void run()
    {
        this.runAndGetResult();
    }

    public boolean runAndGetBooleanResult()
    {
        final Object result = this.runAndGetResult();
        if (result != null)
        {
            LOG.trace("Result type: {}", result.getClass().getName());
            boolean resultEvaluatesToTrue;
            if (result instanceof Boolean)
            {
                resultEvaluatesToTrue = (Boolean) result;
            }
            else if (result instanceof Integer)
            {
                resultEvaluatesToTrue = 0 != (Integer) result;
            }
            else if (result instanceof String)
            {
                resultEvaluatesToTrue = ((String) result).length() > 0;
            }
            else if (result instanceof Float)
            {
                resultEvaluatesToTrue = 0.0f != (Float) result;
            }
            else if (result instanceof Double)
            {
                resultEvaluatesToTrue = 0.0 != (Double) result;
            }
            else
            {
                LOG.debug("Type not handled");
                resultEvaluatesToTrue = false;
            }
            LOG.debug("Expression gave a result of type '{}' which was evaluated to: {}", result.getClass().getName(),
                resultEvaluatesToTrue);
            return resultEvaluatesToTrue;
        }
        else
        {
            LOG.debug("Expression gave null result");
            return false;
        }
    }

    private Object runAndGetResult()
    {
        LOG.trace("Running external script");

        if (this.compiledScript == null)
        {
            if (this.exists())
            {
                throw new RuntimeException("Trying to use script which was not yet compiled");
            }
            else
            {
                return null;
            }
        }
        Object result;
        try
        {
            result = this.compiledScript.eval();
        }
        catch (final ScriptException se)
        {
            throw new ExecutionException("Error during script execution.\n" + se.getMessage());
        }
        LOG.trace("External script finished with result: {}", result);
        return result;
    }
}
