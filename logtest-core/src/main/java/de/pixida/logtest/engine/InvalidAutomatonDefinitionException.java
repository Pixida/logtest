/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

public class InvalidAutomatonDefinitionException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public InvalidAutomatonDefinitionException(final String message)
    {
        super(message);
    }

    public InvalidAutomatonDefinitionException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
