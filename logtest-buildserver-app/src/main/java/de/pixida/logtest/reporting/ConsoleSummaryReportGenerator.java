/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.reporting;

import java.text.DecimalFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.Job;
import de.pixida.logtest.processing.LogSink;

public class ConsoleSummaryReportGenerator implements IReportGenerator
{
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleSummaryReportGenerator.class);

    private int numSucceededExecutions = 0;
    private int numFailedExecutions = 0;
    private final long totalTimeMs;

    public ConsoleSummaryReportGenerator(final long aTotalTimeMs)
    {
        this.totalTimeMs = aTotalTimeMs;
    }

    @Override
    public void start()
    {
        LOG.info("");
        LOG.info("--------------------------- RESULTS --------------------------");
        LOG.info("Summary");
        LOG.info("");
    }

    @Override
    public void pushExecution(final Job job, final LogSink sink, final EvaluationResult evaluationResult, final long timeMs)
    {
        if (evaluationResult.isSuccess())
        {
            LOG.info("{} on {}: SUCCESS", job.getLogReader().getDisplayName(), sink.getDisplayName());
            this.numSucceededExecutions++;
        }
        else if (evaluationResult.isFailure())
        {
            LOG.info("{} on {}: FAILED: {}", job.getLogReader().getDisplayName(), sink.getDisplayName(), evaluationResult.getMessage());
            this.numFailedExecutions++;
        }
        else if (evaluationResult.isError())
        {
            LOG.info("{} on {}: ERROR: {}", job.getLogReader().getDisplayName(), sink.getDisplayName(), evaluationResult.getMessage());
            this.numFailedExecutions++;
        }
    }

    @Override
    public void finish()
    {
        final int totalNumExecutions = this.numSucceededExecutions + this.numFailedExecutions;
        if (totalNumExecutions == 0)
        {
            LOG.info("(no results)");
        }
        LOG.info("--------------------------------------------------------------");
        LOG.info("TESTS {} (successsfully completed {} / {})", this.numFailedExecutions == 0 ? "SUCCEEDED" : "FAILED",
            this.numSucceededExecutions, this.numSucceededExecutions + this.numFailedExecutions);
        LOG.info("--------------------------------------------------------------");
        final double msToSec = 1000.0;
        LOG.info("Total time: {} s", String.format("%.3f", this.totalTimeMs / msToSec));
        LOG.info("Finished at: {}", new Date().toString());
        LOG.info("Final memory consumption: {} / {}", formatSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()),
            formatSize(Runtime.getRuntime().totalMemory()));
        LOG.info("--------------------------------------------------------------");
    }

    private static String formatSize(final long size)
    {
        if (size <= 0L)
        {
            return "0";
        }
        if (size == Long.MAX_VALUE)
        {
            return "unlimited";
        }
        final String[] units = new String[] {"Bi", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};
        final int twotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwo = 1024;
        final int digitGroups = (int) (Math.log10(size)
            / Math.log10(twotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwo));
        return new DecimalFormat("#,##0.#")
            .format(size / Math.pow(twotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwotwothetwo, digitGroups))
            + " " + units[digitGroups];
    }
}
