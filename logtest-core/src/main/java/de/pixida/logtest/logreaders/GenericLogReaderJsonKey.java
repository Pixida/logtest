/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.logreaders;

public enum GenericLogReaderJsonKey
{
    // The keys always hold for the latest version of automaton definitions.
    // *Never change* any of the keys defined here as this will destroy backwards compatibility. Instead, create a method that translates
    // older definitions to the equivalent definition in the current file format and change the set of keys *afterwards*!
    HEADLINE_PATTERN("headlinePattern"),
    HEADLINE_PATTERN_INDEX_OF_TIMESTAMP("headlinePatternIndexOfTimestamp"),
    HEADLINE_PATTERN_INDEX_OF_CHANNEL("headlinePatternIndexOfChannel"),
    TRIM_PAYLOAD("trimPayload"),
    REMOVE_EMPTY_PAYLOAD_LINES_FROM_MULTILINE_ENTRY("removeEmptyPayloadLinesFromMultilineEntry"),
    HANDLING_OF_NON_HEADLINE_LINES("handlingOfNonHeadlineLines"),
    LOG_FILE_CHARSET("logFileCharset");

    private String keyName;

    private GenericLogReaderJsonKey(final String aKeyName)
    {
        this.keyName = aKeyName;
    }

    String getKey()
    {
        return this.keyName;
    }
}
