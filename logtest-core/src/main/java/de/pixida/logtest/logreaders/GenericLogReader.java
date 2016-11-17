/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.logreaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericLogReader implements ILogReader
{
    private static final Logger LOG = LoggerFactory.getLogger(GenericLogReader.class);

    public enum HandlingOfNonHeadlineLines
    {
        FAIL,
        CREATE_MULTILINE_ENTRY,
        ASSUME_LAST_TIMESTAMP,
        ASSUME_LAST_TIMESTAMP_AND_CHANNEL
    }

    private final File logFile;

    private BufferedReader br;
    private String lookaheadLine;

    // Put default configuration here... note: It would be fatal to change them. In this case, define new properties and map old values.
    private String headlinePattern = "^(.*?([0-9]+))";
    private Integer headlinePatternIndexOfTimestamp = 2;
    private Integer headlinePatternIndexOfChannel = null;
    private boolean trimPayload = true;
    private boolean removeEmptyPayloadLinesFromMultilineEntry = true;
    private HandlingOfNonHeadlineLines handlingOfNonHeadlineLines = HandlingOfNonHeadlineLines.FAIL;
    private Charset logFileCharset = StandardCharsets.UTF_8;

    private Pattern compiledHeadlinePattern;

    private long lineNo;
    private long lastTimestamp;
    private String lastChannel;

    private Throwable exception;
    private boolean inputStreamClosed;

    public GenericLogReader(final File aLogFile)
    {
        Validate.notNull(aLogFile);
        this.logFile = aLogFile;
    }

    public GenericLogReader(final BufferedReader aBufferedReader)
    {
        Validate.notNull(aBufferedReader);
        this.br = aBufferedReader;
        this.logFile = null;
    }

    public void setHeadlinePattern(final String value)
    {
        this.headlinePattern = value;
        this.compiledHeadlinePattern = null; // Force recompilation
    }

    public void setTrimPayload(final boolean value)
    {
        this.trimPayload = value;
    }

    public void setRemoveEmptyPayloadLinesFromMultilineEntry(final boolean value)
    {
        this.removeEmptyPayloadLinesFromMultilineEntry = value;
    }

    public void setHandlingOfNonHeadlineLines(final HandlingOfNonHeadlineLines value)
    {
        Validate.notNull(value);
        this.handlingOfNonHeadlineLines = value;
    }

    public void setHeadlinePatternIndexOfTimestamp(final Integer value)
    {
        this.headlinePatternIndexOfTimestamp = value;
    }

    public void setHeadlinePatternIndexOfChannel(final Integer value)
    {
        this.headlinePatternIndexOfChannel = value;
    }

    public void setLogFileCharset(final Charset value)
    {
        Validate.notNull(value);
        this.logFileCharset = value;
    }

    public String getHeadlinePattern()
    {
        return this.headlinePattern;
    }

    public Integer getHeadlinePatternIndexOfTimestamp()
    {
        return this.headlinePatternIndexOfTimestamp;
    }

    public Integer getHeadlinePatternIndexOfChannel()
    {
        return this.headlinePatternIndexOfChannel;
    }

    public boolean getTrimPayload()
    {
        return this.trimPayload;
    }

    public boolean getRemoveEmptyPayloadLinesFromMultilineEntry()
    {
        return this.removeEmptyPayloadLinesFromMultilineEntry;
    }

    public HandlingOfNonHeadlineLines getHandlingOfNonHeadlineLines()
    {
        return this.handlingOfNonHeadlineLines;
    }

    public Charset getLogFileCharset()
    {
        return this.logFileCharset;
    }

    @Override
    public ILogEntry getNextEntry()
    {
        // Check if we did not end with an exception
        if (this.exception != null)
        {
            final String errorMsg = "Cannot deliver next log entry as there was an exception.";
            LOG.error(errorMsg);
            throw new LogReaderException(errorMsg);
        }

        // Check if we're already finished
        if (this.inputStreamClosed)
        {
            LOG.warn("Input stream was already closed manually or there are no more entries as already returned");
            return null;
        }

        // Open log if not yet done
        this.openLogStreamIfNotOpened();

        // Fetch next entry and close resource on exception
        try
        {
            final ILogEntry result = this.getNextEntryInternal();
            if (result == null)
            {
                this.inputStreamClosed = true;
            }
            return result;
        }
        catch (final LogReaderException lre)
        {
            this.closeInputStream();
            this.exception = lre;
            throw lre;
        }
    }

    @Override
    public void close()
    {
        if (!this.inputStreamClosed)
        {
            this.closeInputStream();
            this.inputStreamClosed = true;
        }
    }

    public static boolean checkIfTheFileMightContainAValidConfiguration(final File file)
    {
        try
        {
            return new JSONObject(FileUtils.readFileToString(file, StandardCharsets.UTF_8))
                .has(GenericLogReaderJsonKey.HEADLINE_PATTERN.getKey());
        }
        catch (final IOException ioe)
        {
            LOG.debug("File seems not to be a log reader configuration '{}'", file.getAbsolutePath());
            return false;
        }
    }

    @Override
    public JSONObject getSettingsForConfigurationFile()
    {
        final JSONObject configuration = new JSONObject();
        this.writeStringToConfig(configuration, GenericLogReaderJsonKey.HEADLINE_PATTERN, this.headlinePattern);
        this.writeIntegerToConfig(configuration, GenericLogReaderJsonKey.HEADLINE_PATTERN_INDEX_OF_TIMESTAMP,
            this.headlinePatternIndexOfTimestamp);
        this.writeIntegerToConfig(configuration, GenericLogReaderJsonKey.HEADLINE_PATTERN_INDEX_OF_CHANNEL,
            this.headlinePatternIndexOfChannel);
        this.writeBooleanToConfig(configuration, GenericLogReaderJsonKey.TRIM_PAYLOAD, this.trimPayload);
        this.writeBooleanToConfig(configuration, GenericLogReaderJsonKey.REMOVE_EMPTY_PAYLOAD_LINES_FROM_MULTILINE_ENTRY,
            this.removeEmptyPayloadLinesFromMultilineEntry);
        this.writeStringToConfig(configuration, GenericLogReaderJsonKey.HANDLING_OF_NON_HEADLINE_LINES,
            this.handlingOfNonHeadlineLines == null ? null : this.handlingOfNonHeadlineLines.toString());
        this.writeStringToConfig(configuration, GenericLogReaderJsonKey.LOG_FILE_CHARSET,
            this.logFileCharset == null ? null : this.logFileCharset.name());
        return configuration;
    }

    private void writeStringToConfig(final JSONObject configuration, final GenericLogReaderJsonKey jsonProperty, final String value)
    {
        Validate.notNull(configuration);
        Validate.notNull(jsonProperty);
        if (value != null)
        {
            configuration.put(jsonProperty.getKey(), value);
        }
    }

    private void writeIntegerToConfig(final JSONObject configuration, final GenericLogReaderJsonKey jsonProperty, final Integer value)
    {
        Validate.notNull(configuration);
        Validate.notNull(jsonProperty);
        if (value != null)
        {
            configuration.put(jsonProperty.getKey(), value);
        }
    }

    private void writeBooleanToConfig(final JSONObject configuration, final GenericLogReaderJsonKey jsonProperty, final Boolean value)
    {
        Validate.notNull(configuration);
        Validate.notNull(jsonProperty);
        if (value != null)
        {
            configuration.put(jsonProperty.getKey(), value);
        }
    }

    @Override
    public void overwriteCurrentSettingsWithSettingsInConfigurationFile(final JSONObject configuration)
    {
        this.headlinePattern = this.getStringFromConfig(configuration, GenericLogReaderJsonKey.HEADLINE_PATTERN, this.headlinePattern);
        this.headlinePatternIndexOfTimestamp = this.getIntegerFromConfig(configuration,
            GenericLogReaderJsonKey.HEADLINE_PATTERN_INDEX_OF_TIMESTAMP, this.headlinePatternIndexOfTimestamp);
        this.headlinePatternIndexOfChannel = this.getIntegerFromConfig(configuration,
            GenericLogReaderJsonKey.HEADLINE_PATTERN_INDEX_OF_CHANNEL, this.headlinePatternIndexOfChannel);
        this.trimPayload = this.getBoolFromConfig(configuration, GenericLogReaderJsonKey.TRIM_PAYLOAD, this.trimPayload);
        this.removeEmptyPayloadLinesFromMultilineEntry = this.getBoolFromConfig(configuration,
            GenericLogReaderJsonKey.REMOVE_EMPTY_PAYLOAD_LINES_FROM_MULTILINE_ENTRY, this.removeEmptyPayloadLinesFromMultilineEntry);
        this.readConfiguredHandlingOfNonHeadlineLinesSetting(configuration, GenericLogReaderJsonKey.HANDLING_OF_NON_HEADLINE_LINES);
        this.readConfiguredLogFileCharset(configuration, GenericLogReaderJsonKey.LOG_FILE_CHARSET);
    }

    @Override
    public String getDisplayName()
    {
        if (this.logFile != null)
        {
            return this.logFile.getName();
        }
        else
        {
            return "(unknown source)";
        }
    }

    @Override
    public void open()
    {
        // We open on demand
    }

    private void readConfiguredHandlingOfNonHeadlineLinesSetting(final JSONObject configuration, final GenericLogReaderJsonKey jsonProperty)
    {
        if (configuration.has(jsonProperty.getKey()))
        {
            String value;
            try
            {
                value = configuration.getString(jsonProperty.getKey());
            }
            catch (final JSONException jsonEx)
            {
                throw this.createJsonConfigException("string", jsonProperty.getKey());
            }
            try
            {
                this.handlingOfNonHeadlineLines = Enum.valueOf(HandlingOfNonHeadlineLines.class, value);
            }
            catch (final IllegalArgumentException iae)
            {
                LOG.error("Invalid value for setting '{}': '{}'. Allowed values: {}", jsonProperty.getKey(), value,
                    HandlingOfNonHeadlineLines.values());
                throw new LogReaderException("Invalid value for setting '" + jsonProperty.getKey() + "': '" + value + "'. Allowed values: "
                    + StringUtils.join(HandlingOfNonHeadlineLines.values(), ", "));
            }
        }
    }

    private void readConfiguredLogFileCharset(final JSONObject configuration, final GenericLogReaderJsonKey jsonProperty)
    {
        if (configuration.has(jsonProperty.getKey()))
        {
            String value;
            try
            {
                value = configuration.getString(jsonProperty.getKey());
            }
            catch (final JSONException jsonEx)
            {
                throw this.createJsonConfigException("string", jsonProperty.getKey());
            }
            try
            {
                this.logFileCharset = Charset.forName(value);
            }
            catch (IllegalCharsetNameException | UnsupportedCharsetException e)
            {
                LOG.error("Invalid or unsupported charset for setting '{}': {}", jsonProperty.getKey(), value);
                throw new LogReaderException("Invalid or unsupported charset for setting '" + jsonProperty.getKey() + "': " + value);
            }
        }
    }

    private LogReaderException createJsonConfigException(final String expectedType, final String itemName)
    {
        LOG.error("Invalid type for setting '{}', expecting '{}'", itemName, expectedType);
        return new LogReaderException("Invalid type for setting '" + itemName + "', expecting '" + expectedType + "'");
    }

    private String getStringFromConfig(final JSONObject configuration, final GenericLogReaderJsonKey jsonProperty,
        final String currentValue)
    {
        try
        {
            if (configuration.has(jsonProperty.getKey()))
            {
                return configuration.getString(jsonProperty.getKey());
            }
            else
            {
                return currentValue;
            }
        }
        catch (final JSONException jsonEx)
        {
            throw this.createJsonConfigException("string", jsonProperty.getKey());
        }
    }

    private Integer getIntegerFromConfig(final JSONObject configuration, final GenericLogReaderJsonKey jsonProperty,
        final Integer currentValue)
    {
        try
        {
            if (configuration.has(jsonProperty.getKey()))
            {
                return configuration.isNull(jsonProperty.getKey()) ? null : configuration.getInt(jsonProperty.getKey());
            }
            else
            {
                return currentValue;
            }
        }
        catch (final JSONException jsonEx)
        {
            throw this.createJsonConfigException("integer", jsonProperty.getKey());
        }
    }

    private boolean getBoolFromConfig(final JSONObject configuration, final GenericLogReaderJsonKey jsonProperty,
        final boolean currentValue)
    {
        try
        {
            if (configuration.has(jsonProperty.getKey()))
            {
                return configuration.getBoolean(jsonProperty.getKey());
            }
            else
            {
                return currentValue;
            }
        }
        catch (final JSONException jsonEx)
        {
            throw this.createJsonConfigException("bool", jsonProperty.getKey());
        }
    }

    private void closeInputStream()
    {
        IOUtils.closeQuietly(this.br);
        this.br = null;
    }

    private ILogEntry getNextEntryInternal()
    {
        try
        {
            final String headLine = this.getNextLine();

            if (headLine == null)
            {
                LOG.info("Finished with reading '{}' lines from log '{}'", this.lineNo, this.toString());
                this.closeInputStream();
                return null;
            }

            if (this.headlinePattern == null)
            {
                final String errorMsg = "Cannot extract headline: No pattern set";
                LOG.error(errorMsg);
                throw new LogReaderException(errorMsg);
            }
            this.compiledHeadlinePattern = this.compilePattern(this.headlinePattern, this.compiledHeadlinePattern);

            final Matcher headLineMatcher = this.compiledHeadlinePattern.matcher(headLine);
            final boolean headLineMatches = headLineMatcher.find();
            Long timestampMs = null;
            String channel = null;
            if (!headLineMatches)
            {
                if (this.handlingOfNonHeadlineLines == HandlingOfNonHeadlineLines.ASSUME_LAST_TIMESTAMP)
                {
                    timestampMs = this.lastTimestamp;
                }
                else if (this.handlingOfNonHeadlineLines == HandlingOfNonHeadlineLines.ASSUME_LAST_TIMESTAMP_AND_CHANNEL)
                {
                    timestampMs = this.lastTimestamp;
                    channel = this.lastChannel;
                }
                else
                {
                    LOG.debug("Failed to extract headline from line '{}' as '{}' via '{}'", this.lineNo, headLine, this.headlinePattern);
                    throw new LogReaderException("Failed to extract headline in line " + this.lineNo);
                }

                assert timestampMs != null;
            }
            else
            {
                timestampMs = this.extractTimestampFromHeadLineMatcher(headLineMatcher);
                if (timestampMs == null)
                {
                    LOG.debug("Failed to extract timestamp in line '{}'", this.lineNo);
                    throw new LogReaderException("Failed to extract timestamp in line " + this.lineNo);
                }
                channel = this.extractChannelFromHeadLineMatcher(headLineMatcher);
            }

            final String strippedHeadLine = headLineMatches ? PatternMatchingsStripper.strip(headLineMatcher, headLine) : headLine;

            final long headLineNoBeforeOverwrittenByReadingMultipleLines = this.lineNo;
            final String payload = this.extractPayloadWhenHeadlineIsGiven(strippedHeadLine);

            this.lastTimestamp = timestampMs;
            this.lastChannel = channel;

            final GenericLogEntry result = new GenericLogEntry(headLineNoBeforeOverwrittenByReadingMultipleLines, timestampMs, payload,
                channel);
            LOG.trace("Extracted log entry: {}", result);
            return result;
        }
        catch (final IOException ioe)
        {
            final String errorMsg = "I/O Error";
            LOG.error(errorMsg, ioe);
            throw new LogReaderException(errorMsg, ioe);
        }
    }

    private Long extractTimestampFromHeadLineMatcher(final Matcher headLineMatcher)
    {
        Long timestampMs = null;
        if (this.headlinePatternIndexOfTimestamp == null)
        {
            LOG.trace("Not extracting timestamp; no index assigned");
        }
        else
        {
            final String timestampString = this.getMatch(headLineMatcher, this.headlinePatternIndexOfTimestamp);
            try
            {
                if (timestampString != null)
                {
                    LOG.trace("Timestamp matched: {}", timestampString);
                    timestampMs = Long.parseLong(timestampString);
                }
            }
            catch (final NumberFormatException nfe)
            {
                LOG.error("Invalid timestamp value in line '{}': {}", this.lineNo, timestampString);
                throw new LogReaderException(
                    "Failed to extract timestamp in line '" + this.lineNo + "' - invalid number: " + timestampString);
            }
        }
        return timestampMs;
    }

    private String extractChannelFromHeadLineMatcher(final Matcher headLineMatcher)
    {
        String channel;
        if (this.headlinePatternIndexOfChannel == null)
        {
            LOG.trace("Not extracting channel; no index assigned");
            channel = ILogEntry.DEFAULT_CHANNEL;
        }
        else
        {
            channel = this.getMatch(headLineMatcher, this.headlinePatternIndexOfChannel);
            if (channel == null)
            {
                channel = ILogEntry.DEFAULT_CHANNEL;
            }
        }
        return channel;
    }

    private String getMatch(final Matcher matcher, final Integer groupIndex)
    {
        if (groupIndex > matcher.groupCount())
        {
            throw new LogReaderException(
                "Tried to extract matching group '" + groupIndex + "' but pattern gave only '" + matcher.groupCount() + "' matches");
        }
        return matcher.group(groupIndex);
    }

    private String extractPayloadWhenHeadlineIsGiven(final String headLine) throws IOException
    {
        final List<String> payloadLines = new ArrayList<>();
        payloadLines.add(headLine);
        if (this.handlingOfNonHeadlineLines == HandlingOfNonHeadlineLines.CREATE_MULTILINE_ENTRY)
        {
            this.appendFurtherMultilineEntityLines(payloadLines);
        }
        return this.joinLinesToPayload(payloadLines);
    }

    private String joinLinesToPayload(final List<String> payloadLines)
    {
        String payload;
        LOG.trace("Joining multiline entries (including headline): {}", payloadLines);

        if (this.trimPayload)
        {
            LOG.trace("Trimming payload lines");
            for (int j = 0; j < payloadLines.size(); j++)
            {
                payloadLines.set(j, payloadLines.get(j).trim());
            }
        }
        else
        {
            LOG.trace("Not trimming payload lines");
        }

        if (this.removeEmptyPayloadLinesFromMultilineEntry)
        {
            LOG.trace("Skipping empty payload lines");
            for (final Iterator<String> it = payloadLines.iterator(); it.hasNext();)
            {
                if (it.next().length() == 0)
                {
                    it.remove();
                }
            }
        }
        else
        {
            LOG.trace("Not removing empty payload lines");
        }

        payload = payloadLines.stream().collect(Collectors.joining("\n"));

        LOG.trace("Payload lines assembled to payload: {}", payloadLines, payload);
        return payload;
    }

    private void openLogStreamIfNotOpened()
    {
        if (this.br == null)
        {
            try
            {
                this.br = new BufferedReader(new InputStreamReader(new FileInputStream(this.logFile), this.logFileCharset));
                this.lineNo = 0L;
                this.lastTimestamp = 0L;
                this.lastChannel = ILogEntry.DEFAULT_CHANNEL;
                this.inputStreamClosed = false;
            }
            catch (final FileNotFoundException e)
            {
                // Append file path into exception as the message is the result of the test and allows for quicker error diagnosis
                String filePath;
                try
                {
                    filePath = this.logFile.getCanonicalPath();
                }
                catch (final IOException ioe)
                {
                    filePath = this.logFile.getAbsolutePath();
                }
                throw new LogReaderException("Cannot open log file: " + filePath, e);
            }
        }
    }

    private void appendFurtherMultilineEntityLines(final List<String> result) throws IOException
    {
        // Proceed until the headline pattern matches
        for (;;)
        {
            final String nextLine = this.getNextLine();

            if (nextLine == null)
            {
                return;
            }

            final boolean newEntry = this.compilePattern(this.headlinePattern, this.compiledHeadlinePattern).matcher(nextLine).find();
            if (newEntry)
            {
                LOG.trace("Taking line '{}' for NEW entry", this.lineNo);
                this.saveLookahead(nextLine);
                return;
            }
            else
            {
                LOG.trace("Appending line to multiline entity in line '{}' as pattern does not match", this.lineNo);
                result.add(nextLine);
            }
        }
    }

    private Pattern compilePattern(final String pattern, final Pattern precompiledPattern)
    {
        if (precompiledPattern == null)
        {
            try
            {
                return Pattern.compile(pattern);
            }
            catch (final PatternSyntaxException pse)
            {
                throw new LogReaderException("Invalid configured regular expression: " + pattern, pse);
            }
        }
        return precompiledPattern;
    }

    private String getNextLine() throws IOException
    {
        String result;
        if (this.lookaheadLine != null)
        {
            result = this.lookaheadLine;
            this.lookaheadLine = null;
        }
        else
        {
            result = this.br.readLine();
        }
        if (result != null)
        {
            this.lineNo++;
        }
        return result;
    }

    private void saveLookahead(final String value)
    {
        Validate.isTrue(this.lookaheadLine == null, "Internal error - can only store one lookahead line");
        Validate.notNull(value);
        this.lookaheadLine = value;
        this.lineNo--;
    }

    // Just for logging output / no business use
    @Override
    public String toString()
    {
        if (this.logFile != null)
        {
            return this.logFile.getAbsolutePath();
        }
        else
        {
            return "<external unknown stream source>";
        }
    }
}
