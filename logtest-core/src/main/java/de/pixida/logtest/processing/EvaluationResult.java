/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.processing;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class EvaluationResult
{
    public enum Result
    {
        SUCCESS,
        FAILURE,
        AUTOMATON_DEFECT,
        INTERNAL_ERROR
    }

    private final Result result;
    private final String message;

    public EvaluationResult(final Result aResult, final String aMessage)
    {
        this.result = aResult;
        this.message = aMessage;
    }

    public Result getResult()
    {
        return this.result;
    }

    public String getMessage()
    {
        return this.message;
    }

    public boolean isSuccess()
    {
        return this.result == Result.SUCCESS;
    }

    public boolean isFailure()
    {
        return this.result == Result.FAILURE;
    }

    public boolean isError()
    {
        return this.result == Result.AUTOMATON_DEFECT || this.result == Result.INTERNAL_ERROR;
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
