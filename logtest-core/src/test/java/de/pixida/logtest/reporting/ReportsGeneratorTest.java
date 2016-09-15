/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.reporting;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.Job;
import de.pixida.logtest.processing.JobExecutor;
import de.pixida.logtest.processing.JobExecutorTest;
import de.pixida.logtest.processing.LogSink;

public class ReportsGeneratorTest
{
    private static class TestReportGenerator implements IReportGenerator
    {
        private boolean started = false;
        private boolean finished = false;
        private int numExecutions = 0;

        TestReportGenerator()
        {
            // Empty constructor needed by checkstyle
        }

        @Override
        public void start()
        {
            this.started = true;
        }

        @Override
        public void pushExecution(final Job job, final LogSink sink, final EvaluationResult evaluationResult, final long timeMs)
        {
            this.numExecutions++;
        }

        @Override
        public void finish()
        {
            this.finished = true;
        }

        public boolean isStarted()
        {
            return this.started;
        }

        public boolean isFinished()
        {
            return this.finished;
        }

        public int getNumExecutions()
        {
            return this.numExecutions;
        }
    }

    public ReportsGeneratorTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testReportGeneratorIsCalledProperly()
    {
        final List<Job> jobs = JobExecutorTest.createSomeStupidJobs();
        final JobExecutor je = new JobExecutor(jobs);
        final List<List<EvaluationResult>> results = je.getResults();
        final ReportsGenerator rg = new ReportsGenerator();
        rg.setJobs(jobs);
        rg.setResults(results);
        rg.setJobExecutionTimes(Collections.nCopies(jobs.size(), 1L));
        final TestReportGenerator testReportGenerator = new TestReportGenerator();
        rg.addReportGenerator(testReportGenerator);
        rg.generateReports();

        Assert.assertTrue(testReportGenerator.isStarted());
        Assert.assertEquals(jobs.stream().mapToLong(job -> job.getSinks().size()).sum(), testReportGenerator.getNumExecutions());
        Assert.assertTrue(testReportGenerator.isFinished());
    }

    @Test
    public void testReportGeneratorIsCalledProperlyWhenJobListIsEmpty()
    {
        final List<Job> jobs = Collections.emptyList();
        final JobExecutor je = new JobExecutor(jobs);
        final List<List<EvaluationResult>> results = je.getResults();
        final ReportsGenerator rg = new ReportsGenerator();
        rg.setJobs(jobs);
        rg.setResults(results);
        rg.setJobExecutionTimes(Collections.nCopies(jobs.size(), 1L));
        final TestReportGenerator testReportGenerator = new TestReportGenerator();
        rg.addReportGenerator(testReportGenerator);
        rg.generateReports();

        Assert.assertTrue(testReportGenerator.isStarted());
        Assert.assertEquals(0, testReportGenerator.getNumExecutions());
        Assert.assertTrue(testReportGenerator.isFinished());
    }
}
