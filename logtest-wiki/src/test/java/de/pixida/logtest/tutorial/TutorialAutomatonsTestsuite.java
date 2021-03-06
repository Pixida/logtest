/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.tutorial;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
                TutorialAutomatonsTest.class
})
public class TutorialAutomatonsTestsuite
{
    public TutorialAutomatonsTestsuite()
    {
        // Empty constructor needed by checkstyle
    }
}
