/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.engine;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                AutomatonTest.class,
                EmbeddedScriptTest.class,
                PregCallbackReplacerTest.class,
                ScriptingEngineTest.class
})
public class EngineTestsuite
{
    public EngineTestsuite()
    {
        // Empty constructor needed by checkstyle
    }
}
