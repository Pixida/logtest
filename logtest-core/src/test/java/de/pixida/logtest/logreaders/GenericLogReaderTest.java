/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.logreaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import de.pixida.logtest.logreaders.GenericLogReader.HandlingOfNonHeadlineLines;

public class GenericLogReaderTest
{
    private static final long ONE = 1;
    private static final long TWO = 2;
    private static final long THREE = 3;

    private static final String TEST_PATTERN = "[0-9]+";
    private static final Integer TEST_HEADLINE_PATTERN_INDEX_OF_TIMESTAMP = 100;
    private static final Integer TEST_HEADLINE_PATTERN_INDEX_OF_CHANNEL = 150;
    private static final Boolean TEST_TRIM_PAYLOAD = false;
    private static final Boolean TEST_REMOVE_EMPTY_PAYLOAD_LINES_FROM_MULTILINE_ENTRY = false;
    private static final HandlingOfNonHeadlineLines TEST_HANDLING_OF_NON_HEADLINE_LINES = GenericLogReader.HandlingOfNonHeadlineLines.ASSUME_LAST_TIMESTAMP;
    private static final String TEST_LOG_FILE_CHARSET = "ISO-8859-1";

    private GenericLogReader lr;

    public GenericLogReaderTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testBasicUseCase()
    {
        final String log = "1 C Entry 1\n"
            + "2 D Entry 2\n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.checkNextResult(ONE, ONE, "C", "Entry 1");
        this.checkNextResult(TWO, TWO, "D", "Entry 2");
        this.assumeNoMoreEntries();
    }

    @Test(expected = LogReaderException.class)
    public void testInvalidPatternThrowsException()
    {
        final String log = "A\n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("(");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.getNextEntry();
    }

    @Test
    public void testParsingMultilineEntry()
    {
        final String log = "1 C Entry 1\n"
            + "Entry 1\n"
            + "2 D Entry 2";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.CREATE_MULTILINE_ENTRY);
        this.checkNextResult(ONE, ONE, "C", "Entry 1\nEntry 1");
        this.checkNextResult(THREE, TWO, "D", "Entry 2");
        this.assumeNoMoreEntries();
    }

    @Test
    public void testParsingLogFromFile()
    {
        final File testLog = new File(this.getClass().getResource("test-log.txt").getFile());
        this.lr = new GenericLogReader(testLog);
        this.lr.open();
        Assert.assertEquals(testLog, new File(this.lr.toString()));
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.CREATE_MULTILINE_ENTRY);
        this.checkNextResult(ONE, ONE, "A", "Entry 1");
        this.checkNextResult(TWO, TWO, "B", "Entry 2");
    }

    @Test(expected = LogReaderException.class)
    public void testExceptionIsThrownIfLogFileDoesNotExist()
    {
        final File testLog = new File(new File(this.getClass().getResource("test-log.txt").getFile()).getAbsolutePath() + "___");
        this.lr = new GenericLogReader(testLog);
        this.lr.open();
        Assert.assertEquals(testLog, new File(this.lr.toString()));
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.CREATE_MULTILINE_ENTRY);
        this.checkNextResult(ONE, ONE, "A", "Entry 1");
        this.checkNextResult(TWO, TWO, "B", "Entry 2");
    }

    @Test(expected = LogReaderException.class)
    public void testInvalidMultilineEntriesAreDetected()
    {
        final String log = "1 C Entry 1\n"
            + "Entry 2\n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.FAIL);
        this.checkNextResult(ONE, ONE, "C", "Entry 1");
        this.lr.getNextEntry();
    }

    @Test
    public void testHeadLinesOnlyWithRecentTimestamp()
    {
        final String log = "1 C Entry 1\n"
            + "Entry 2\n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.ASSUME_LAST_TIMESTAMP);
        this.checkNextResult(ONE, ONE, "C", "Entry 1");
        this.checkNextResult(TWO, ONE, ILogEntry.DEFAULT_CHANNEL, "Entry 2");
        this.assumeNoMoreEntries();
    }

    @Test
    public void testHeadLinesOnlyWithRecentTimestampAndChannel()
    {
        final String log = "1 C Entry 1\n"
            + "Entry 2\n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.ASSUME_LAST_TIMESTAMP_AND_CHANNEL);
        this.checkNextResult(ONE, ONE, "C", "Entry 1");
        this.checkNextResult(TWO, ONE, "C", "Entry 2");
        this.assumeNoMoreEntries();
    }

    @Test
    public void testPayloadIsTrimmedInHeadLines()
    {
        final String log = "1 C Entry 1 ";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^([0-9]+) ([A-Z])");
        this.lr.setHeadlinePatternIndexOfTimestamp(1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1);
        this.lr.setTrimPayload(true);
        this.checkNextResult(ONE, ONE, "C", "Entry 1");
        this.assumeNoMoreEntries();
    }

    @Test
    public void testPayloadIsTrimmedInHeadLinesAndSubseedingLines()
    {
        final String log = "1 C Entry 1 \n"
            + " Entry 2 \n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^([0-9]+) ([A-Z]) ");
        this.lr.setHeadlinePatternIndexOfTimestamp(1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.CREATE_MULTILINE_ENTRY);
        this.lr.setTrimPayload(true);
        this.checkNextResult(ONE, ONE, "C", "Entry 1\nEntry 2");
        this.assumeNoMoreEntries();
    }

    @Test
    public void testEmptyMultilineEntryLinesCanBeNotRemoved()
    {
        final String log = "1 C Entry 1\n\n"
            + "Entry 2\n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.CREATE_MULTILINE_ENTRY);
        this.lr.setRemoveEmptyPayloadLinesFromMultilineEntry(false);
        this.checkNextResult(ONE, ONE, "C", "Entry 1\n\nEntry 2");
        this.assumeNoMoreEntries();
    }

    @Test
    public void testEmptyMultilineEntryLinesCanBeRemoved()
    {
        final String log = "1 C Entry 1\n\n"
            + "Entry 2\n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setHandlingOfNonHeadlineLines(HandlingOfNonHeadlineLines.CREATE_MULTILINE_ENTRY);
        this.lr.setRemoveEmptyPayloadLinesFromMultilineEntry(true);
        this.checkNextResult(ONE, ONE, "C", "Entry 1\nEntry 2");
        this.assumeNoMoreEntries();
    }

    @Test
    public void testEmptyPayloadLinesAreNoSkipped()
    {
        final String log = "1 C Entry 1\n"
            + "2 D \n";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^(([0-9]+) ([A-Z]) )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1 + 1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.lr.setRemoveEmptyPayloadLinesFromMultilineEntry(true);
        this.checkNextResult(ONE, ONE, "C", "Entry 1");
        this.checkNextResult(TWO, TWO, "D", "");
        this.assumeNoMoreEntries();
    }

    @Test
    public void testDisplayNameShowsFileName()
    {
        this.initLogReader("");
        Assert.assertEquals("(unknown source)", this.lr.getDisplayName());
        this.lr = new GenericLogReader(new File("/some/file.txt"));
        Assert.assertEquals("file.txt", this.lr.getDisplayName());
    }

    @Test
    public void testIfNoChannelsAreDefinedAllEntriesAreAssignedToDefaultChannel()
    {
        final String log = "1 Entry 1";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^([0-9]+) ");
        this.lr.setHeadlinePatternIndexOfTimestamp(1);
        this.assumeNextEntryHasChannel(ILogEntry.DEFAULT_CHANNEL);
        this.assumeNoMoreEntries();
    }

    @Test
    public void testChannelsCanBeOptionallyDefined()
    {
        final String log = "1 Entry 1\n"
            + "2 C Entry 2";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^([0-9]+)( ([A-Z]))?( )");
        this.lr.setHeadlinePatternIndexOfTimestamp(1);
        this.lr.setHeadlinePatternIndexOfChannel(1 + 1 + 1);
        this.assumeNextEntryHasChannel(ILogEntry.DEFAULT_CHANNEL);
        this.checkNextResult(TWO, TWO, "C", "Entry 2");
        this.assumeNoMoreEntries();
    }

    @Test(expected = LogReaderException.class)
    public void testTimestampCanNotBeOptionallyDefined()
    {
        final String log = "Entry 1";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("^([0-9]+)?");
        this.lr.setHeadlinePatternIndexOfTimestamp(1);
        this.lr.getNextEntry();
    }

    @Test
    public void testPullingAfterTheLastLineWasReadRepeadinglyReturnsThatThereAreNoMoreLines()
    {
        final String log = "1";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("([0-9])");
        this.lr.setHeadlinePatternIndexOfTimestamp(1);
        Assert.assertNotNull(this.lr.getNextEntry());
        Assert.assertNull(this.lr.getNextEntry());
        Assert.assertNull(this.lr.getNextEntry());
    }

    @Test
    public void testReaderCanBeClosedBeforeEverythingWasRead()
    {
        final String log = "1\n2";
        this.initLogReader(log);
        this.lr.setHeadlinePattern("([0-9])");
        this.lr.setHeadlinePatternIndexOfTimestamp(1);
        Assert.assertNotNull(this.lr.getNextEntry());
        this.lr.close();
        Assert.assertNull(this.lr.getNextEntry());
    }

    @Test
    public void testOverwritingConfiguration()
    {
        this.initLogReader("");

        this.lr.overwriteCurrentSettingsWithSettingsInConfigurationFile(this.createTestConfiguationJsonConfig());

        this.validateCurrentConfigurationDoesNotEqualDefaultConfiguration();
        this.validateTestConfigurationIsSet();
    }

    @Test
    public void testExportingConfiguration()
    {
        this.initLogReader("");

        this.lr.overwriteCurrentSettingsWithSettingsInConfigurationFile(this.createTestConfiguationJsonConfig());
        final JSONObject jsonConfiguration = this.lr.getSettingsForConfigurationFile();

        this.initLogReader("");
        this.lr.overwriteCurrentSettingsWithSettingsInConfigurationFile(jsonConfiguration);

        this.validateTestConfigurationIsSet();
    }

    /*
     * Use this method to see if all configuration items are really applied. This requires that the test configuration does not equal the
     * default configuration.
     */
    private void validateCurrentConfigurationDoesNotEqualDefaultConfiguration()
    {
        final GenericLogReader virgin = new GenericLogReader(new BufferedReader(new StringReader("")));
        Assert.assertTrue(!TEST_PATTERN.equals(virgin.getHeadlinePattern()));
        Assert.assertTrue(!TEST_HEADLINE_PATTERN_INDEX_OF_TIMESTAMP.equals(virgin.getHeadlinePatternIndexOfTimestamp()));
        Assert.assertTrue(!TEST_HEADLINE_PATTERN_INDEX_OF_CHANNEL.equals(virgin.getHeadlinePatternIndexOfChannel()));
        Assert.assertTrue(TEST_TRIM_PAYLOAD != virgin.getTrimPayload());
        Assert.assertTrue(TEST_REMOVE_EMPTY_PAYLOAD_LINES_FROM_MULTILINE_ENTRY != virgin.getRemoveEmptyPayloadLinesFromMultilineEntry());
        Assert.assertTrue(!TEST_HANDLING_OF_NON_HEADLINE_LINES.equals(virgin.getHandlingOfNonHeadlineLines()));
        Assert.assertTrue(!TEST_LOG_FILE_CHARSET.equals(virgin.getLogFileCharset()));
    }

    private JSONObject createTestConfiguationJsonConfig()
    {
        final JSONObject configuration = new JSONObject();
        configuration.put(GenericLogReaderJsonKey.HEADLINE_PATTERN.getKey(), TEST_PATTERN);
        configuration.put(GenericLogReaderJsonKey.HEADLINE_PATTERN_INDEX_OF_TIMESTAMP.getKey(), TEST_HEADLINE_PATTERN_INDEX_OF_TIMESTAMP);
        configuration.put(GenericLogReaderJsonKey.HEADLINE_PATTERN_INDEX_OF_CHANNEL.getKey(), TEST_HEADLINE_PATTERN_INDEX_OF_CHANNEL);
        configuration.put(GenericLogReaderJsonKey.TRIM_PAYLOAD.getKey(), TEST_TRIM_PAYLOAD);
        configuration.put(GenericLogReaderJsonKey.REMOVE_EMPTY_PAYLOAD_LINES_FROM_MULTILINE_ENTRY.getKey(),
            TEST_REMOVE_EMPTY_PAYLOAD_LINES_FROM_MULTILINE_ENTRY);
        configuration.put(GenericLogReaderJsonKey.HANDLING_OF_NON_HEADLINE_LINES.getKey(), TEST_HANDLING_OF_NON_HEADLINE_LINES.toString());
        configuration.put(GenericLogReaderJsonKey.LOG_FILE_CHARSET.getKey(), TEST_LOG_FILE_CHARSET);
        return configuration;
    }

    private void validateTestConfigurationIsSet()
    {
        Assert.assertEquals(TEST_PATTERN, this.lr.getHeadlinePattern());
        Assert.assertEquals(TEST_HEADLINE_PATTERN_INDEX_OF_TIMESTAMP, this.lr.getHeadlinePatternIndexOfTimestamp());
        Assert.assertEquals(TEST_HEADLINE_PATTERN_INDEX_OF_CHANNEL, this.lr.getHeadlinePatternIndexOfChannel());
        Assert.assertEquals(TEST_TRIM_PAYLOAD, this.lr.getTrimPayload());
        Assert.assertEquals(TEST_REMOVE_EMPTY_PAYLOAD_LINES_FROM_MULTILINE_ENTRY, this.lr.getRemoveEmptyPayloadLinesFromMultilineEntry());
        Assert.assertEquals(TEST_HANDLING_OF_NON_HEADLINE_LINES, this.lr.getHandlingOfNonHeadlineLines());
        Assert.assertEquals(Charset.forName(TEST_LOG_FILE_CHARSET), this.lr.getLogFileCharset());
    }

    private void initLogReader(final String log)
    {
        final BufferedReader br = new BufferedReader(new StringReader(log));
        this.lr = new GenericLogReader(br);
        this.lr.open();
    }

    private void checkNextResult(final long lineNumber, final long time, final String channel, final String payload)
    {
        final ILogEntry e = this.lr.getNextEntry();
        Assert.assertEquals(lineNumber, e.getLineNumber());
        Assert.assertEquals(time, e.getTime());
        Assert.assertEquals(channel, e.getChannel());
        Assert.assertEquals(payload, e.getPayload());
    }

    private void assumeNextEntryHasChannel(final String channel)
    {
        Assert.assertEquals(channel, this.lr.getNextEntry().getChannel());
    }

    private void assumeNoMoreEntries()
    {
        Assert.assertNull(this.lr.getNextEntry());
    }
}
