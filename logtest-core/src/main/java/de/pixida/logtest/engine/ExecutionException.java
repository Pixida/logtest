/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

public class ExecutionException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public ExecutionException(final String message)
    {
        super(message);
    }
}
