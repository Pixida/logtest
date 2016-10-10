/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import org.junit.Test;

public class AutomatonDefinitionToJsonConverterTest
{
    public AutomatonDefinitionToJsonConverterTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testSaveRestoreTestAutomatonDefinition()
    {
        final JsonAutomatonDefinition r = SomeTestAutomaton.getAutomatonDefinition();
        SomeTestAutomaton.verifyAutomatonDefinition(r); // Just to make sure the verification works on original automaton

        final String automatonConvertedToJson = AutomatonDefinitionToJsonConverter.convert(r).toString();

        final JsonAutomatonDefinition r2 = new JsonAutomatonDefinition("automaton after conversion", automatonConvertedToJson);
        SomeTestAutomaton.verifyAutomatonDefinition(r2);
    }
}
