/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.reporting;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.Job;

public class ReportsGenerator
{
    private static final Logger LOG = LoggerFactory.getLogger(ReportsGenerator.class);

    private List<Job> jobs;
    private List<Long> jobExecutiontimesMs;
    private List<List<EvaluationResult>> results;
    private final List<IReportGenerator> reportGenerators = new ArrayList<>();

    public ReportsGenerator()
    {
        // Empty constructor needed by checkstyle
    }

    public void setJobs(final List<Job> value)
    {
        Validate.notNull(value);
        this.jobs = value;
    }

    public void setResults(final List<List<EvaluationResult>> value)
    {
        Validate.notNull(value);
        this.results = value;
    }

    public void setJobExecutionTimes(final List<Long> value)
    {
        Validate.notNull(value);
        this.jobExecutiontimesMs = value;
    }

    public void addReportGenerator(final IReportGenerator value)
    {
        Validate.notNull(value);
        this.reportGenerators.add(value);
    }

    public void generateReports()
    {
        Validate.notNull(this.jobs, "Jobs mut be set before generating report!");
        Validate.notNull(this.results, "Results mut be set before generating report!");

        if (this.reportGenerators.size() == 0)
        {
            LOG.warn("Generating reports, but there are no report generators!");
        }

        LOG.trace("Generating reports...");

        this.reportGenerators.forEach(reportGenerator -> reportGenerator.start());
        for (int i = 0; i < this.jobs.size(); i++)
        {
            for (int j = 0; j < this.results.get(i).size(); j++)
            {
                final int job = i;
                final int sink = j;
                this.reportGenerators.forEach(reportGenerator -> reportGenerator.pushExecution(this.jobs.get(job),
                    this.jobs.get(job).getSinks().get(sink), this.results.get(job).get(sink), this.jobExecutiontimesMs.get(job)));
            }
        }
        this.reportGenerators.forEach(reportGenerator -> reportGenerator.finish());

        LOG.trace("Report generation completed");
    }
}
