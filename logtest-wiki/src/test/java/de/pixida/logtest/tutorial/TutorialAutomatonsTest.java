/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.tutorial;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import de.pixida.logtest.automatondefinitions.JsonAutomatonDefinition;
import de.pixida.logtest.logreaders.GenericLogReader;
import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.Job;
import de.pixida.logtest.processing.JobExecutor;
import de.pixida.logtest.processing.LogSink;

public class TutorialAutomatonsTest
{
    public TutorialAutomatonsTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testTestFridgeStartsUpCorrectly()
    {
        Assert.assertTrue(this.getExecutionResultOfAutomatonOnTutorialLog("fridge-starts-up-correctly"));
    }

    @Test
    public void testTestFridgeRunsSteadilyTrivial()
    {
        Assert.assertFalse(this.getExecutionResultOfAutomatonOnTutorialLog("fridge-runs-steadily"));
    }

    @Test
    public void testTestFridgeRunsSteadilyFixed()
    {
        Assert.assertTrue(this.getExecutionResultOfAutomatonOnTutorialLog("fridge-runs-steadily-fixed"));
    }

    private boolean getExecutionResultOfAutomatonOnTutorialLog(final String automatonName)
    {
        final String tutorialFolder = "/tutorial";

        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new StringReader(
                FileUtils.readFileToString(new File(this.getClass().getResource(tutorialFolder + "/log.txt").getFile()),
                    StandardCharsets.UTF_8)));
        }
        catch (final IOException e)
        {
            Assert.fail(e.getMessage());
        }

        final LogSink sink = new LogSink();
        try
        {
            sink.setAutomaton(
                new JsonAutomatonDefinition(new File(this.getClass().getResource(tutorialFolder + "/" + automatonName + ".json").toURI())));
        }
        catch (final URISyntaxException e)
        {
            Assert.fail(e.getMessage());
        }
        sink.setParameters(Collections.emptyMap());

        final Job job = new Job();
        job.setLogReader(new GenericLogReader(br));
        job.setSinks(Arrays.asList(sink));

        final JobExecutor jobExecutor = new JobExecutor(Arrays.asList(job));
        final List<List<EvaluationResult>> results = jobExecutor.getResults();

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(1, results.get(0).size());
        return results.get(0).get(0).isSuccess();
    }
}
