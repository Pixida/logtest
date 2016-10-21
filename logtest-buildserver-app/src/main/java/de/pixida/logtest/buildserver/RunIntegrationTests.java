/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.buildserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.pixida.logtest.automatondefinitions.JsonAutomatonDefinition;
import de.pixida.logtest.logreaders.GenericLogReader;
import de.pixida.logtest.logreaders.ILogReader;
import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.Job;
import de.pixida.logtest.processing.JobExecutor;
import de.pixida.logtest.processing.LogSink;
import de.pixida.logtest.reporting.ConsoleSummaryReportGenerator;
import de.pixida.logtest.reporting.JUnitStyleXmlReportGenerator;
import de.pixida.logtest.reporting.ReportsGenerator;

public class RunIntegrationTests
{
    private static final String AUTOMATON_DIRECTORY_SWITCH = "automatonDirectory";
    private static final String TRACE_LOG_DIRECTORY_SWITCH = "traceLogDirectory";
    private static final String VERBOSITY_SWITCH = "verbose";
    private static final String REPORT_SWITCH = "reportFile";
    private static final String LOG_READER_CONFIG_SWITCH = "logReaderConfig";
    private static final String LOG_READER_CONFIG_FILE_SWITCH = "logReaderConfigFile";
    private static final String DEFAULT_PARAMETER_FILE_SWITCH = "defaultParameterFile";
    private static final String HELP_SWITCH = "help";

    private static final Logger LOG = LoggerFactory.getLogger(RunIntegrationTests.class);

    static class ExitWithFailureException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        ExitWithFailureException()
        {
            // Empty constructor needed by checkstyle
        }

        int getExitCode()
        {
            return 1;
        }
    }

    private boolean verbose = false;
    private File jUnitReportTarget = null;
    private Map<File, List<Pair<File, Map<String, String>>>> configuredExecutions;
    private List<List<EvaluationResult>> results;
    private List<Job> jobs;
    private final StopWatch stopWatch = new StopWatch();
    private List<Long> jobExecutionTimesMs;
    private JSONObject logReaderConfigFromCommandLine;
    private JSONObject logReaderConfigFromFile;
    private final Map<String, String> defaultParameters = new HashMap<>();

    public RunIntegrationTests()
    {
        // Empty constructor needed by checkstyle
    }

    public static void main(final String[] args)
    {
        try
        {
            final RunIntegrationTests runner = new RunIntegrationTests();
            final boolean run = runner.parseCommandLine(args);
            if (run)
            {
                runner.createAndRunJobs();
                runner.printResults();
            }
        }
        catch (final ExitWithFailureException ee)
        {
            LOG.debug("Finished with exit code: " + ee.getExitCode());
            System.exit(ee.getExitCode());
        }
        catch (final Exception re)
        {
            LOG.debug("Abording with errors", re);
            System.exit(1); // Exit with error
        }
    }

    boolean parseCommandLine(final String[] args)
    {
        final Options options = createOptions();
        final CommandLineParser parser = new DefaultParser();
        try
        {
            final CommandLine params = parser.parse(options, args);
            if (params.hasOption(HELP_SWITCH))
            {
                printHelp(options);
                return false;
            }

            this.applyVerbositySwitch(params);

            this.loadDefaultParameters(params);

            this.configuredExecutions = this.groupAutomatonsByTraceFile(params);

            final String param = params.getOptionValue(REPORT_SWITCH);
            if (param != null)
            {
                this.jUnitReportTarget = new File(param);
            }

            try
            {
                if (params.hasOption(LOG_READER_CONFIG_SWITCH))
                {
                    this.logReaderConfigFromCommandLine = new JSONObject(params.getOptionValue(LOG_READER_CONFIG_SWITCH));
                }
            }
            catch (final JSONException jsonEx)
            {
                throw new ParseException("Failed to parse log reader configuration JSON data from command line: " + jsonEx.getMessage());
            }

            File logReaderConfigurationFile = null;
            try
            {
                if (params.hasOption(LOG_READER_CONFIG_FILE_SWITCH))
                {
                    logReaderConfigurationFile = new File(params.getOptionValue(LOG_READER_CONFIG_FILE_SWITCH));
                    this.logReaderConfigFromFile = new JSONObject(
                        IOUtils.toString(logReaderConfigurationFile.toURI(), StandardCharsets.UTF_8));
                    LOG.debug("Using log reader configuration file '{}'", logReaderConfigurationFile.getCanonicalPath());
                }
                else
                {
                    LOG.debug("No log reader configuration file set.");
                }
            }
            catch (final JSONException jsonEx)
            {
                throw new ParseException("Failed to parse log reader configuration JSON data from file: " + jsonEx.getMessage());
            }
            catch (final IOException e)
            {
                throw new ParseException("Failed to read log reader configuration file '" + logReaderConfigurationFile.getAbsolutePath()
                    + "': " + e.getMessage());
            }
        }
        catch (final ParseException e)
        {
            // CHECKSTYLE:OFF We intentionally print to STDERR here
            System.err.println(e.getMessage());
            System.err.println();
            printHelp(options);
            // CHECKSTYLE:ON

            // Abort with failure - build server job must not succeed if the calling convention is erroneous
            throw new ExitWithFailureException();
        }

        return true;
    }

    boolean getIsVerbose()
    {
        return this.verbose;
    }

    void createAndRunJobs()
    {
        this.jobs = this.createJobs(this.configuredExecutions);
        LOG.info("Starting integration tests");
        this.stopWatch.start();
        final JobExecutor executor = new JobExecutor(this.jobs);
        this.results = executor.getResults();
        this.jobExecutionTimesMs = executor.getJobExecutionTimesMs();
        this.stopWatch.stop();
        LOG.info("Integration tests finished");
    }

    void printResults()
    {
        final ReportsGenerator reportsGenerator = new ReportsGenerator();
        reportsGenerator.setJobs(this.jobs);
        reportsGenerator.setResults(this.results);
        reportsGenerator.addReportGenerator(new ConsoleSummaryReportGenerator(this.stopWatch.getTime()));
        reportsGenerator.setJobExecutionTimes(this.jobExecutionTimesMs);
        if (this.jUnitReportTarget != null)
        {
            reportsGenerator.addReportGenerator(new JUnitStyleXmlReportGenerator(this.jUnitReportTarget, this.stopWatch.getTime()));
        }
        reportsGenerator.generateReports();
        final long numFailedExecutions = this.results.stream().mapToLong(result -> result.stream().filter(er -> !er.isSuccess()).count())
            .sum();

        if (numFailedExecutions == 1)
        {
            throw new ExitWithFailureException();
        }
    }

    private void loadDefaultParameters(final CommandLine params) throws ParseException
    {
        File defaultParameterFile = null;
        try
        {
            if (params.hasOption(DEFAULT_PARAMETER_FILE_SWITCH))
            {
                defaultParameterFile = new File(params.getOptionValue(DEFAULT_PARAMETER_FILE_SWITCH));
                final JSONObject root = new JSONObject(IOUtils.toString(defaultParameterFile.toURI(), StandardCharsets.UTF_8));
                for (final String k : root.keySet())
                {
                    final String v = root.getString(k);
                    this.defaultParameters.put(k, v);
                }
                LOG.debug("Loaded '{}' default parameters from file '{}'", this.defaultParameters.size(),
                    defaultParameterFile.getCanonicalPath());
                LOG.trace("Using default parameters: {}", this.defaultParameters);
            }
            else
            {
                LOG.debug("No default parameters set.");
            }
        }
        catch (final JSONException jsonEx)
        {
            throw new ParseException("Failed to parse default parameter JSON data from file: " + jsonEx.getMessage());
        }
        catch (final IOException e)
        {
            throw new ParseException(
                "Failed to read default parameter file '" + defaultParameterFile.getAbsolutePath() + "': " + e.getMessage());
        }
    }

    private List<Job> createJobs(final Map<File, List<Pair<File, Map<String, String>>>> pairsWithParams)
    {
        final List<Job> result = new ArrayList<>();
        for (final Entry<File, List<Pair<File, Map<String, String>>>> pair : pairsWithParams.entrySet())
        {
            final List<LogSink> sinks = new ArrayList<>();
            for (final Pair<File, Map<String, String>> sinkDef : pair.getValue())
            {
                final LogSink newSink = new LogSink();
                newSink.setAutomaton(new JsonAutomatonDefinition(sinkDef.getLeft()));
                newSink.setParameters(sinkDef.getRight());
                sinks.add(newSink);
            }

            final Job newJob = new Job();
            newJob.setLogReader(this.createAndConfigureLogReader(pair.getKey()));
            newJob.setSinks(sinks);
            result.add(newJob);
        }
        return result;
    }

    private void applyVerbositySwitch(final CommandLine params)
    {
        if (params.hasOption(VERBOSITY_SWITCH))
        {
            if (org.apache.log4j.Logger.getRootLogger().getLevel().isGreaterOrEqual(Level.DEBUG)) // Don't turn TRACE into DEBUG
            {
                org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
                this.verbose = true;
                LOG.debug("Verbose mode enabled");
            }
        }
    }

    private static Options createOptions()
    {
        final Options options = new Options();
        final Option traceLogDirectory = Option.builder("t")
            .longOpt(TRACE_LOG_DIRECTORY_SWITCH)
            .desc("Trace logs location")
            .hasArg()
            .argName("folder")
            .build();
        final Option automatonDirectory = Option.builder("a")
            .longOpt(AUTOMATON_DIRECTORY_SWITCH)
            .desc("Automatons location")
            .hasArg()
            .argName("folder")
            .build();
        final Option reportFile = Option.builder("r")
            .longOpt(REPORT_SWITCH)
            .desc("Generated JUnit report XML file target location")
            .hasArg()
            .argName("file")
            .build();
        final Option verbosity = Option.builder("v")
            .longOpt(VERBOSITY_SWITCH)
            .desc("Enable debug output")
            .build();
        final Option logReaderConfigSwitch = Option.builder("lrcfg")
            .longOpt(LOG_READER_CONFIG_SWITCH)
            .desc("Log reader configuration (JSON)")
            .hasArg()
            .argName("json-object")
            .build();
        final Option logReaderConfigFileSwitch = Option.builder("lrcfgf")
            .longOpt(LOG_READER_CONFIG_FILE_SWITCH)
            .desc("Log reader configuration file")
            .hasArg()
            .argName("file")
            .build();
        final Option defaultParameterFileSwitch = Option.builder("dpf")
            .longOpt(DEFAULT_PARAMETER_FILE_SWITCH)
            .desc("Default parameter file")
            .hasArg()
            .argName("file")
            .build();
        final Option helpSwitch = Option.builder("h")
            .longOpt(HELP_SWITCH)
            .desc("Show (this) help only")
            .build();
        options.addOption(traceLogDirectory);
        options.addOption(automatonDirectory);
        options.addOption(logReaderConfigSwitch);
        options.addOption(logReaderConfigFileSwitch);
        options.addOption(defaultParameterFileSwitch);
        options.addOption(reportFile);
        options.addOption(verbosity);
        options.addOption(helpSwitch);
        return options;
    }

    private ILogReader createAndConfigureLogReader(final File logFile)
    {
        final GenericLogReader logReader = new GenericLogReader(logFile);

        // Settings from configuration file
        if (this.logReaderConfigFromFile != null)
        {
            logReader.overwriteCurrentSettingsWithSettingsInConfigurationFile(this.logReaderConfigFromFile);
        }

        // Settings from command line
        if (this.logReaderConfigFromCommandLine != null)
        {
            logReader.overwriteCurrentSettingsWithSettingsInConfigurationFile(this.logReaderConfigFromCommandLine);
        }

        return logReader;
    }

    private Map<File, List<Pair<File, Map<String, String>>>> groupAutomatonsByTraceFile(final CommandLine params)
        throws ParseException
    {
        final File logFolder = new File(commandLineParamOrCurrentDirectory(params, TRACE_LOG_DIRECTORY_SWITCH));
        final File automatonsFolder = new File(commandLineParamOrCurrentDirectory(params, AUTOMATON_DIRECTORY_SWITCH));
        LOG.debug("Using log folder: {}", logFolder.getAbsolutePath());
        LOG.debug("Using automatons folder: {}", automatonsFolder.getAbsolutePath());

        final Map<File, List<Pair<File, Map<String, String>>>> result = new HashMap<>();
        for (final String arg : params.getArgList())
        {
            final int numComponentsLogFileAndAutomaton = 2;
            final int numComponentsLogFileAndAutomatonAndParameter = 3;
            final String[] components = arg.split(":", numComponentsLogFileAndAutomatonAndParameter);
            if (components.length < numComponentsLogFileAndAutomaton || components.length > numComponentsLogFileAndAutomatonAndParameter)
            {
                throw new ParseException(
                    "Invalid execution entry on command line. Format must be <logfile>:<automaton>[:<parameters>]: " + arg);
            }
            final File traceLog = new File(logFolder, components[0]);
            List<Pair<File, Map<String, String>>> automatons = result.get(traceLog);
            if (automatons == null)
            {
                automatons = new ArrayList<>();
                result.put(traceLog, automatons);
            }
            Map<String, String> parameters = null;
            if (components.length >= numComponentsLogFileAndAutomatonAndParameter)
            {
                parameters = this.parseAutomatonParameters(components[numComponentsLogFileAndAutomatonAndParameter - 1]);
            }
            if (parameters == null)
            {
                parameters = this.parseAutomatonParameters("");
            }

            automatons.add(Pair.of(new File(automatonsFolder, components[1]), parameters));
        }
        return result;
    }

    private static String commandLineParamOrCurrentDirectory(final CommandLine params, final String paramName)
    {
        return params.hasOption(paramName) ? params.getOptionValue(paramName) : ".";
    }

    private Map<String, String> parseAutomatonParameters(final String string) throws ParseException
    {
        final Map<String, String> result = new HashMap<>();

        // Add default parameters
        result.putAll(this.defaultParameters);

        if (string.length() > 0)
        {
            // Separate by ','
            final String[] params = string.split(",");

            // Separate by '='
            for (final String param : params)
            {
                final int kvLen = 2;
                final String[] kv = param.split("=", kvLen);
                if (kv.length != kvLen)
                {
                    throw new ParseException("A parameter entry must be a key=value pair.");
                }
                result.put(kv[0], kv[1]);
            }
        }

        return result;
    }

    private static void printHelp(final Options options)
    {
        final HelpFormatter formatter = new HelpFormatter();
        final int assumedConsoleWidth = 150;
        formatter.setWidth(assumedConsoleWidth);
        formatter.printHelp("java -jar logtest-buildserver-app.jar [OPTIONS]... [EXECUTIONS]...\n"
            + "An EXECUTION is a triple <scenario-filename>:<automaton-filename>[:<parameters, comma separated key=value pairs...>,<...>]"
            + " e.g. tracelog.txt:checkSystemStartupSucceeds.json:waitForNetIO=yes,timeout=30"
            + "\n", // Separate help text from description of parameters with an empty line
            options);
    }

    Map<File, List<Pair<File, Map<String, String>>>> getConfiguredExecutions()
    {
        return this.configuredExecutions;
    }
}
