/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.reporting;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.EvaluationResult.Result;
import de.pixida.logtest.processing.Job;
import de.pixida.logtest.processing.JobExecutorTest;

public class ConsoleSummaryReportGeneratorTest
{
    public ConsoleSummaryReportGeneratorTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testReportGeneratesValidOutput() throws IOException
    {
        final long totalTimeMs = 1000;
        final ConsoleSummaryReportGenerator generator = new ConsoleSummaryReportGenerator(totalTimeMs);
        final List<Job> jobs = JobExecutorTest.createSomeStupidJobs();
        final EvaluationResult[] possibleResults = new EvaluationResult[Result.values().length];
        possibleResults[0] = new EvaluationResult(Result.SUCCESS, null);
        possibleResults[1] = new EvaluationResult(Result.FAILURE, "some failure");
        possibleResults[1 + 1] = new EvaluationResult(Result.INTERNAL_ERROR, "internal error");
        possibleResults[1 + 1 + 1] = new EvaluationResult(Result.AUTOMATON_DEFECT, "automaton is defect");
        // We must have enough tests to iterate through all result variants. I.e. this is an assertion to test if this test is meaningful.
        Assert.assertTrue(possibleResults.length <= jobs.stream().mapToLong(job -> job.getSinks().size()).sum());
        for (final EvaluationResult possibleResult : possibleResults)
        {
            if (possibleResult == null)
            {
                Assert.fail("Possible results must have a result for all result types!");
            }
        }
        int totalI = 0;
        final LoggerListener phisher = new LoggerListener();
        try
        {
            Logger.getLogger(ConsoleSummaryReportGenerator.class).addAppender(phisher);
            generator.start();
            for (int i = 0; i < jobs.size(); i++)
            {
                for (int j = 0; j < jobs.get(i).getSinks().size(); j++)
                {
                    generator.pushExecution(jobs.get(i), jobs.get(i).getSinks().get(j), possibleResults[totalI % possibleResults.length],
                        1L);
                    totalI++;
                }
            }
            generator.finish();
        }
        finally
        {
            Logger.getLogger(ConsoleSummaryReportGenerator.class).removeAppender(phisher);
        }

        // Check some output
        Assert.assertTrue(phisher.getLogEntries().size() > 0);
        this.checkOutputContainsStringInLogEntry(phisher, "RESULTS");
        this.checkOutputContainsStringInLogEntry(phisher, "Summary");
        this.checkOutputContainsStringInLogEntry(phisher, "TESTS FAILED (successsfully completed 1 / 4)");
        this.checkOutputContainsStringInLogEntry(phisher, "Total time: 1,000 s");
    }

    private void checkOutputContainsStringInLogEntry(final LoggerListener phisher, final String string)
    {
        Assert.assertTrue("Log output must contain string in any entry: " + string,
            phisher.getLogEntries().stream().filter(entry -> entry.getRight().contains(string)).count() > 0);
    }
}
