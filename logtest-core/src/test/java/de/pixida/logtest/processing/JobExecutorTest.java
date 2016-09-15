/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.processing;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import de.pixida.logtest.automatondefinitions.GenericNode;
import de.pixida.logtest.automatondefinitions.IAutomatonDefinition;
import de.pixida.logtest.automatondefinitions.INodeDefinition;
import de.pixida.logtest.automatondefinitions.TestAutomaton;
import de.pixida.logtest.logreaders.GenericLogEntry;
import de.pixida.logtest.logreaders.GenericLogReader;
import de.pixida.logtest.logreaders.ILogEntry;
import de.pixida.logtest.logreaders.ILogReader;
import de.pixida.logtest.processing.EvaluationResult.Result;

public class JobExecutorTest
{
    public JobExecutorTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testSomeStupidJobsRunProperly()
    {
        final List<Job> jobs = createSomeStupidJobs();
        final JobExecutor je = new JobExecutor(jobs);
        Assert.assertEquals(jobs.size(), je.getResults().size());
        Assert.assertEquals(jobs.size(), je.getJobExecutionTimesMs().size());
        // The following test is a timing issue: Mostly it succeeds, but sometimes the condition fail as jobs are executed in "0ms"
        //        Assert.assertTrue(je.getJobExecutionTimesMs().stream().filter(t -> t == 0L).count() == 0);
        Assert.assertTrue(je.getJobExecutionTimesMs().stream().filter(t -> t < 0L).count() == 0); // At least check no value is negative
    }

    public static List<Job> createSomeStupidJobs()
    {
        final TestAutomaton invalidAutomaton1 = new TestAutomaton();
        invalidAutomaton1.createNode();
        final TestAutomaton invalidAutomaton2 = new TestAutomaton();
        invalidAutomaton2.createNode();
        final TestAutomaton invalidAutomaton3 = new TestAutomaton();
        invalidAutomaton3.createNode();
        final TestAutomaton invalidAutomaton4 = new TestAutomaton();
        invalidAutomaton4.createNode();

        final List<IAutomatonDefinition> automatons1 = Arrays.asList(invalidAutomaton1, invalidAutomaton2);
        final List<IAutomatonDefinition> automatons2 = Arrays.asList(invalidAutomaton3, invalidAutomaton4);
        final GenericLogReader input1 = new GenericLogReader(new BufferedReader(new StringReader("1 HELLO\n2 WORLD")));
        final GenericLogReader input2 = new GenericLogReader(new BufferedReader(new StringReader("")));

        final List<Job> jobs = new ArrayList<>();
        final Job job1 = new Job();
        job1.setLogReader(input1);
        job1.setSinks(automatons1.stream().map(automaton -> {
            final LogSink sink = new LogSink();
            sink.setAutomaton(automaton);
            sink.setParameters(Collections.emptyMap());
            return sink;
        }).collect(Collectors.toList()));
        jobs.add(job1);
        final Job job2 = new Job();
        job2.setLogReader(input2);
        job2.setSinks(automatons2.stream().map(automaton -> {
            final LogSink sink = new LogSink();
            sink.setAutomaton(automaton);
            sink.setParameters(Collections.emptyMap());
            return sink;
        }).collect(Collectors.toList()));
        jobs.add(job2);
        return jobs;
    }

    @Test
    public void runSomeEvaluationWithSimultaneousEvaluatedSucceedingAndNonSucceedingAndInvalidAutomatons()
    {
        final TestAutomaton ta1 = new TestAutomaton();
        final GenericNode initial1 = ta1.createNode().withFlags(EnumSet.of(INodeDefinition.Flag.IS_INITIAL)).get();
        final GenericNode success1 = ta1.createNode().withFlags(EnumSet.of(INodeDefinition.Flag.IS_SUCCESS)).get();
        ta1.createEdge(initial1, success1).withRegExp("HELLO");

        final TestAutomaton ta2 = new TestAutomaton();
        final GenericNode initial2 = ta2.createNode().withFlags(EnumSet.of(INodeDefinition.Flag.IS_INITIAL)).get();
        final GenericNode middle2 = ta2.createNode().get();
        final GenericNode success2 = ta2.createNode().get();
        ta2.createEdge(initial2, middle2).withRegExp("H");
        ta2.createEdge(middle2, success2).withRegExp("W");

        final TestAutomaton invalidAutomaton = new TestAutomaton();
        invalidAutomaton.createNode();

        final List<LogSink> sinks = Arrays.asList(ta1, ta2, invalidAutomaton).stream().map(automaton -> {
            final LogSink sink = new LogSink();
            sink.setAutomaton(automaton);
            sink.setParameters(Collections.emptyMap());
            return sink;
        }).collect(Collectors.toList());

        final GenericLogReader logReader = new GenericLogReader(new BufferedReader(new StringReader("1 HELLO\n2 WORLD")));
        logReader.setHeadlinePattern("^(.*?([0-9]+))");
        logReader.setHeadlinePatternIndexOfTimestamp(1 + 1);

        final JobExecutor jobExecutor = this.createJobExecutor(sinks, logReader);

        final List<List<EvaluationResult>> jobResults = jobExecutor.getResults();
        final List<EvaluationResult> results = jobResults.get(0);

        Assert.assertEquals(sinks.size(), results.size());
        Assert.assertTrue(results.get(0).getResult() == Result.SUCCESS);
        Assert.assertNull(results.get(0).getMessage());
        Assert.assertFalse(results.get(1).getResult() == Result.SUCCESS);
        Assert.assertNotNull(results.get(1).getMessage());
        Assert.assertFalse(results.get(1 + 1).getResult() == Result.SUCCESS);
        Assert.assertNotNull(results.get(1 + 1).getMessage());
    }

    private JobExecutor createJobExecutor(final List<LogSink> sinks, final ILogReader logReader)
    {
        final Job job = new Job();
        job.setLogReader(logReader);
        job.setSinks(sinks);

        final JobExecutor jobExecutor = new JobExecutor(Arrays.asList(job));
        return jobExecutor;
    }

    @Test
    public void testCountingEventsByChannelWorks()
    {
        final TestAutomaton ta = new TestAutomaton();
        final GenericNode node1 = ta.createNode().withFlags(EnumSet.of(INodeDefinition.Flag.IS_INITIAL)).get();
        final GenericNode node2 = ta.createNode().get();
        ta.createEdge(node1, node2).withTriggerAlways();

        final TestLogReader logMessages = new TestLogReader();
        logMessages.addEntry(new GenericLogEntry(1, 1, "HELLO WORLD!", "A"));
        logMessages.addEntry(new GenericLogEntry(1, 1, "HELLO WORLD!", "A"));
        logMessages.addEntry(new GenericLogEntry(1, 1, "HELLO WORLD!"));

        final LogSink sink = new LogSink();
        sink.setAutomaton(ta);
        sink.setParameters(Collections.emptyMap());

        final JobExecutor jobExecutor = this.createJobExecutor(Arrays.asList(sink), logMessages);

        Assert.assertEquals(new Integer(1 + 1), jobExecutor.getNumEventsByChannel().get("A"));
        Assert.assertTrue(!jobExecutor.getNumEventsByChannel().containsKey("XXX"));
        Assert.assertEquals(new Integer(1), jobExecutor.getNumEventsByChannel().get(ILogEntry.DEFAULT_CHANNEL));
    }
}
