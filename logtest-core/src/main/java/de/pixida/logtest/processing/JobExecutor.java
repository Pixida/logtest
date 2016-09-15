/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.engine.Automaton;
import de.pixida.logtest.engine.ExecutionException;
import de.pixida.logtest.logreaders.ILogEntry;
import de.pixida.logtest.logreaders.ILogReader;
import de.pixida.logtest.processing.EvaluationResult.Result;

/** Run n jobs */
public class JobExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger(JobExecutor.class);

    private final List<List<EvaluationResult>> results;
    private final List<Long> jobExecutionTimesMs;
    private final Map<String, int[]> numEventsByChannel = new HashMap<>();
    private boolean evaluationPrematurelyEnded;

    public JobExecutor(final List<Job> jobs)
    {
        Validate.notNull(jobs);
        LOG.info("Starting execution of '{}' jobs", jobs.size());

        this.results = new ArrayList<>(jobs.size());
        this.jobExecutionTimesMs = new ArrayList<>(jobs.size());

        int i = 0;
        for (final Job job : jobs)
        {
            LOG.debug("Starting execution of job '{}'", i);
            final StopWatch watch = new StopWatch();
            watch.start();

            try
            {
                final ILogReader logReader = job.getLogReader();
                Validate.notNull(logReader);
                final List<LogSink> sinks = job.getSinks();
                Validate.notNull(sinks);

                this.runEvaluations(logReader, this.createAutomatons(sinks));
            }
            catch (final RuntimeException re)
            {
                LOG.error("Internal error in job '{}': {}", i, re.getMessage());
                final List<EvaluationResult> errorResultList = new ArrayList<>(job.getSinks().size());
                for (int j = 0; j < job.getSinks().size(); j++)
                {
                    errorResultList.add(new EvaluationResult(Result.INTERNAL_ERROR, re.getMessage()));
                }
                this.results.add(errorResultList);
            }
            this.jobExecutionTimesMs.add(watch.getTime());

            LOG.info("Job {} / {} finished", i + 1, jobs.size());
            i++;
        }

        LOG.debug("Finished execution of '{}' jobs", jobs.size());
    }

    public List<List<EvaluationResult>> getResults()
    {
        return this.results;
    }

    public List<Long> getJobExecutionTimesMs()
    {
        return this.jobExecutionTimesMs;
    }

    private void showNumLogLinesByChannel(final List<Automaton> automatons)
    {
        if (LOG.isDebugEnabled())
        {
            final Map<String, Integer> map = this.getNumEventsByChannel();
            final String nameOfDefaultChannel = "(default)";
            if (!map.containsKey(nameOfDefaultChannel))
            {
                map.put(nameOfDefaultChannel, map.get(ILogEntry.DEFAULT_CHANNEL));
                map.remove(ILogEntry.DEFAULT_CHANNEL);
            }
            String note = "";
            if (this.evaluationPrematurelyEnded)
            {
                note = " (probably does not reflect whole log as job prematurely ended as all automatons were done)";
            }
            LOG.debug("Num log messages grouped by channel{}: {}", note, map);
        }
    }

    private void countEventChannel(final ILogEntry logEntry)
    {
        final String channelName = logEntry.getChannel();
        if (!this.numEventsByChannel.containsKey(channelName))
        {
            this.numEventsByChannel.put(channelName, new int[1]);
        }
        this.numEventsByChannel.get(channelName)[0]++;
    }

    public Map<String, Integer> getNumEventsByChannel()
    {
        final Map<String, Integer> result = new HashMap<>();
        for (final Entry<String, int[]> pair : this.numEventsByChannel.entrySet())
        {
            result.put(pair.getKey(), pair.getValue()[0]);
        }
        return result;
    }

    private List<Automaton> createAutomatons(final List<LogSink> sinks)
    {
        LOG.debug("Creating automatons");
        Validate.notNull(sinks);
        final List<Automaton> automatons = new ArrayList<>(sinks.size());
        for (final LogSink sink : sinks)
        {
            try
            {
                final Automaton newAutomaton = new Automaton(sink.getAutomaton(), sink.getParameters());
                automatons.add(newAutomaton);
            }
            catch (final RuntimeException re)
            {
                LOG.error("Unexpected error while loading automaton '{}' with parameters '{}'",
                    sink.getAutomaton(), sink.getParameters(), re);
                throw re;
            }
        }
        LOG.debug("'{}' automaton(s) successfully created", automatons.size());
        return automatons;
    }

    private void runEvaluations(final ILogReader logReader, final List<Automaton> automatons)
    {
        LOG.info("Starting analysis: Source '{}', simultaneous automatons: {}", logReader,
            automatons.stream().map(automaton -> automaton == null ? "INVALID_AUTOMATON_DEFINITION" : automaton.toString())
                .collect(Collectors.joining(", ")));
        Validate.notNull(logReader);
        Validate.notNull(automatons);
        try
        {
            logReader.open();
            this.pipeLogEntriesIntoAutomatons(logReader, automatons);
        }
        finally
        {
            logReader.close();
        }
        this.results.add(this.collectResults(automatons));
        LOG.debug("Analysis finished. Results: {}", this.results.get(this.results.size() - 1));
    }

    private List<EvaluationResult> collectResults(final List<Automaton> automatons)
    {
        LOG.debug("Collecting results");
        Validate.notNull(automatons);
        final List<EvaluationResult> jobResults = new ArrayList<>(automatons.size());
        for (final Automaton automaton : automatons)
        {
            EvaluationResult.Result result;
            if (automaton.succeeded())
            {
                result = Result.SUCCESS;
            }
            else if (automaton.automatonDefect())
            {
                result = Result.AUTOMATON_DEFECT;
            }
            else
            {
                result = Result.FAILURE;
            }
            jobResults.add(new EvaluationResult(result, automaton.getErrorReason()));
        }
        LOG.debug("Results collected");
        return jobResults;
    }

    private void pipeLogEntriesIntoAutomatons(final ILogReader logReader, final List<Automaton> automatons)
    {
        Validate.notNull(logReader);
        Validate.notNull(automatons);
        for (;;)
        {
            final ILogEntry nextLogEntry = logReader.getNextEntry();
            if (nextLogEntry == null)
            {
                LOG.debug("No more log entries");
                LOG.debug("Pushing EOF");
                automatons.stream().filter(automaton -> automaton != null).forEach(automaton -> automaton.pushEof());
                LOG.debug("EOF pushed");
                LOG.debug("Finishing execution");
                this.showNumLogLinesByChannel(automatons);
                break;
            }
            this.countEventChannel(nextLogEntry);
            int numAutomatonsWhichCanProceed = 0;
            for (final Automaton automaton : automatons)
            {
                if (automaton == null) // Error during initialization of automaton
                {
                    continue;
                }

                try
                {
                    automaton.proceedWithLogEntry(nextLogEntry);
                }
                catch (final ExecutionException ee)
                {
                    LOG.info("Execution exception in automaton '{}': {}", automaton, ee.getMessage());
                }
                catch (final RuntimeException re)
                {
                    LOG.error("Unexpected exception in automaton '{}'", automaton, re);
                }

                if (automaton.canProceed())
                {
                    numAutomatonsWhichCanProceed++;
                }
            }
            if (numAutomatonsWhichCanProceed == 0)
            {
                LOG.info("No more automatons that are running");
                this.evaluationPrematurelyEnded = true;
                break;
            }
        }
    }
}
