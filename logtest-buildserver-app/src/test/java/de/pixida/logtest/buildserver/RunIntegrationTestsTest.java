/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.buildserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.pixida.logtest.buildserver.RunIntegrationTests.ExitWithFailureException;

public class RunIntegrationTestsTest
{
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    public RunIntegrationTestsTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testInvalidCommandLinesAreReportedButParsingDoesNotFailWithABadException()
    {
        this.checkCommandLineParsingFails(new String[] {"missing arguments are counted as error"});
    }

    @Test
    public void testCorrectFormattingOfAutomatonExecutionsIsChecked()
    {
        this.checkCommandLineParsingFails(new String[] {"-a", "a", "-t", "t", "t"});
        this.checkCommandLineParsingFails(new String[] {"-a", "a", "-t", "t", "t:a:b:c"});
        this.checkCommandLineParsingFails(new String[] {"-a", "a", "-t", "t", "t:a:b"});
        this.checkCommandLineParsingFails(new String[] {"-a", "a", "-t", "t", "t:a:b++"});
        new RunIntegrationTests().parseCommandLine(new String[] {"-a", "a", "-t", "t", "t:a:b=1"});
        this.checkCommandLineParsingFails(new String[] {"-a", "a", "-t", "t", "t:a:b=1, "});
        new RunIntegrationTests().parseCommandLine(new String[] {"-a", "a", "-t", "t", "t:a:"});
        new RunIntegrationTests().parseCommandLine(new String[] {"-a", "a", "-t", "t", "t:a:b=1,c=5"});
    }

    @Test
    public void testVerbosityFlagIsApplied()
    {
        RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(new String[] {"-a", "a", "-t", "t"});
        Assert.assertFalse(r.getIsVerbose());
        r = new RunIntegrationTests();
        r.parseCommandLine(new String[] {"-a", "a", "-t", "t", "-v"});
        Assert.assertTrue(r.getIsVerbose());
        r = new RunIntegrationTests();
        r.parseCommandLine(new String[] {"-a", "a", "-t", "t", "--verbose"});
        Assert.assertTrue(r.getIsVerbose());
    }

    @Test
    public void testAutomatonShouldNotRunWhenHelpIsPrinted()
    {
        final RunIntegrationTests r = new RunIntegrationTests();
        boolean run = r.parseCommandLine(new String[] {"-h"});
        Assert.assertFalse(run);
        run = r.parseCommandLine(new String[] {});
        Assert.assertTrue(run);
    }

    @Test
    public void testInputFilesAreFound()
    {
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(new String[] {"-a", automatonsPath, "-t", tracesPath, "trace0-success.txt:test-no-bluescreen-appeared.json"});
        final Map<File, List<Pair<File, Map<String, String>>>> executions = r.getConfiguredExecutions();
        final Set<File> expectedTraces = new HashSet<>(Arrays.asList(new File(tracesPath, "trace0-success.txt")));
        Assert.assertTrue(expectedTraces.containsAll(executions.keySet()) && executions.keySet().containsAll(expectedTraces));
        Assert.assertEquals(new File(automatonsPath, "test-no-bluescreen-appeared.json"),
            executions.get(expectedTraces.iterator().next()).get(0).getLeft());
        Assert.assertEquals(0, executions.get(expectedTraces.iterator().next()).get(0).getRight().size());
    }

    @Test
    public void testAutomatonParametersAreApplied()
    {
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(
            new String[] {"-a", automatonsPath, "-t", tracesPath, "trace0-success.txt:test-minimum-runtime.json:minimumRuntimeMs=1000"});
        final Map<File, List<Pair<File, Map<String, String>>>> executions = r.getConfiguredExecutions();
        Assert.assertEquals("1000", executions.get(new File(tracesPath, "trace0-success.txt")).get(0).getRight().get("minimumRuntimeMs"));
    }

    @Test
    public void testSuccessfullyRunningTestScenario()
    {
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(new String[] {"-a", automatonsPath, "-t", tracesPath,
                        "trace0-success.txt:test-minimum-runtime.json:minimumRuntimeMs=31000",
                        "trace0-success.txt:test-no-bluescreen-appeared.json"});
        r.createAndRunJobs();
        r.printResults();
    }

    @Test(expected = ExitWithFailureException.class)
    public void testSuccessfullyRunningFailingTestScenario()
    {
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(new String[] {"-a", automatonsPath, "-t", tracesPath,
                        "trace0-success.txt:test-minimum-runtime.json:minimumRuntimeMs=31000",
                        "trace0-success.txt:test-no-bluescreen-appeared.json",
                        "trace1-bluescreen.txt:test-network-address-change-complete.json",
                        "trace1-bluescreen.txt:test-no-bluescreen-appeared.json"});
        r.createAndRunJobs();
        r.printResults();
    }

    @Test
    public void testJUnitReportFileIsWrittenCorrectly() throws IOException
    {
        final File jUnitOutputFile = this.tempFolder.newFile();
        Assert.assertTrue(jUnitOutputFile.exists());
        Assert.assertTrue(jUnitOutputFile.length() == 0L);
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(new String[] {"-a", automatonsPath, "-t", tracesPath, "-r", jUnitOutputFile.getAbsolutePath(),
                        "trace0-success.txt:test-minimum-runtime.json:minimumRuntimeMs=31000",
                        "trace0-success.txt:test-no-bluescreen-appeared.json"});
        r.createAndRunJobs();
        r.printResults();
        Assert.assertTrue(jUnitOutputFile.exists());
        Assert.assertTrue(jUnitOutputFile.length() > 0L);
    }

    @Test
    public void testExecutionWithoutJobs() throws IOException
    {
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(new String[] {"-a", automatonsPath, "-t", tracesPath});
        r.createAndRunJobs();
        r.printResults();
    }

    @Test
    public void testLogReaderConfigCanBeSetOnCommandLine()
    {
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(
            new String[] {"-a", automatonsPath, "-t", tracesPath,
                            "-lrcfg",
                            "{ 'headlinePattern':'(.*?([0-9]+) (C))',"
                                + "'headlinePatternIndexOfTimestamp':2,"
                                + "'headlinePatternIndexOfChannel':3 }",
                            "one-line-channel-c.txt:expect-at-least-one-logline-of-channel.json"});
        r.createAndRunJobs();
        r.printResults();
    }

    @Test
    public void testLogReaderConfigCanBeSetViaConfigurationFile()
    {
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final String logReaderConfigsPath = this.getTestScenarioPath("test-scenario/log-reader-configs");
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(
            new String[] {"-a", automatonsPath, "-t", tracesPath,
                            "-lrcfgf", logReaderConfigsPath + "/timestamp-and-channel.json",
                            "one-line-channel-c.txt:expect-at-least-one-logline-of-channel.json"});
        r.createAndRunJobs();
        r.printResults();
    }

    @Test
    public void testDefaultParametersAreApplied()
    {
        final String automatonsPath = this.getTestScenarioPath("test-scenario/automatons");
        final String tracesPath = this.getTestScenarioPath("test-scenario/traces");
        final String defaultParamsPath = this.getTestScenarioPath("test-scenario/default-parameters");
        String[] cmdLineParams = new String[] {"-a", automatonsPath, "-t", tracesPath, "some-event-with-5ms.txt:minimum-event-time-t.json"};

        try
        {
            this.runCommandLine(cmdLineParams);
            Assert.fail("Expecting exception due to undefined parameter");
        }
        catch (final ExitWithFailureException ewfe)
        {
            // OK
        }

        cmdLineParams = ArrayUtils.add(cmdLineParams, "-dpf");
        cmdLineParams = ArrayUtils.add(cmdLineParams, defaultParamsPath + "/t-is-5.json");
        this.runCommandLine(cmdLineParams);
    }

    private void runCommandLine(final String[] cmdLineParams)
    {
        final RunIntegrationTests r = new RunIntegrationTests();
        r.parseCommandLine(cmdLineParams);
        r.createAndRunJobs();
        r.printResults();
    }

    private String getTestScenarioPath(final String relativePath)
    {
        return new File(this.getClass().getResource(relativePath).getPath()).getAbsolutePath();
    }

    private void checkCommandLineParsingFails(final String[] args)
    {
        final RunIntegrationTests app = new RunIntegrationTests();
        try
        {
            app.parseCommandLine(args);
        }
        catch (final ExitWithFailureException ee)
        {
            // OK
            return;
        }
        Assert.fail("Command line parsing was assumed to fail, but it succeeded");
    }
}
