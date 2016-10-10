/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JsonAutomatonDefinitionTest
{
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    public JsonAutomatonDefinitionTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testCorrectReadingOfTestAutomatonDefinition()
    {
        final JsonAutomatonDefinition r = SomeTestAutomaton.getAutomatonDefinition();
        SomeTestAutomaton.verifyAutomatonDefinition(r);
    }

    @Test
    public void testDisplayNameIsFileNameOfAutomaton()
    {
        final JsonAutomatonDefinition r = new JsonAutomatonDefinition(
            new File(this.getClass().getResource(SomeTestAutomaton.TEST_FILE_NAME).getFile()));
        r.load();
        Assert.assertEquals(SomeTestAutomaton.TEST_FILE_NAME, r.getDisplayName());
        Assert.assertEquals(SomeTestAutomaton.TEST_FILE_NAME, r.toString());
    }

    @Test(expected = AutomatonLoadingException.class)
    public void testExceptionIsThrownWhenSyntaxIsInvalid() throws IOException
    {
        final File rewrittenConfig = this.testFolder.newFile();
        FileUtils.writeStringToFile(rewrittenConfig, "{", JsonAutomatonDefinition.EXPECTED_CHARSET);
        this.loadAutomatonDefinitionFromFile(rewrittenConfig);
    }

    @Test(expected = AutomatonLoadingException.class)
    public void testExceptionIsThrownIfADestinationNodeIsNotFound() throws IOException
    {
        final JSONObject config = SomeTestAutomaton.getRawConfigJson();
        config.getJSONArray("nodes").getJSONObject(0).getJSONArray("outgoingEdges").getJSONObject(0)
            .put("destination", "INVALID_DEST_NODE");
        final File rewrittenConfig = this.testFolder.newFile();
        FileUtils.writeStringToFile(rewrittenConfig, config.toString(), JsonAutomatonDefinition.EXPECTED_CHARSET);
        this.loadAutomatonDefinitionFromFile(rewrittenConfig);
    }

    @Test(expected = AutomatonLoadingException.class)
    public void testExceptionIsThrownIfARequiredConditionIsInvalid() throws JSONException, IOException
    {
        final JSONObject config = SomeTestAutomaton.getRawConfigJson();
        config.getJSONArray("nodes").getJSONObject(0).getJSONArray("outgoingEdges").getJSONObject(0)
            .put("requiredConditions", "INVALID_VALUE");
        final File rewrittenConfig = this.testFolder.newFile();
        FileUtils.writeStringToFile(rewrittenConfig, config.toString(), JsonAutomatonDefinition.EXPECTED_CHARSET);
        this.loadAutomatonDefinitionFromFile(rewrittenConfig);
    }

    @Test
    public void testWeAreTolerantWhenSourceFileHasInvalidEncoding() throws JSONException, IOException
    {
        final JSONObject config = SomeTestAutomaton.getRawConfigJson();
        config.getJSONArray("nodes").getJSONObject(0).put("id", "Ö"); // Chose UTF-8 multibyte character
        final File rewrittenConfig = this.testFolder.newFile();
        FileUtils.writeStringToFile(rewrittenConfig, config.toString(), StandardCharsets.ISO_8859_1);
        this.loadAutomatonDefinitionFromFile(rewrittenConfig);
    }

    private void loadAutomatonDefinitionFromFile(final File rewrittenConfig)
    {
        new JsonAutomatonDefinition(rewrittenConfig).load();
    }
}
