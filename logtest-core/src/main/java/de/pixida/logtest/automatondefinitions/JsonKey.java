/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

public enum JsonKey
{
    // The keys always hold for the latest version of automaton definitions.
    // *Never change* any of the keys defined here as this will destroy backwards compatibility. Instead, create a method that translates
    // older definitions to the equivalent definition in the current file format and change the set of keys *afterwards*!
    ROOT_ONLOAD("onLoad"),
    ROOT_COMMENT("comment"),
    ROOT_SCRIPT_LANGUAGE("scriptLanguage"),
    ROOT_NODES("nodes"),
    NODE_ID("id"),
    NODE_INITIAL("initial"),
    NODE_FAILURE("failure"),
    NODE_SUCCESS("success"),
    NODE_ON_ENTER("onEnter"),
    NODE_ON_LEAVE("onLeave"),
    NODE_SUCCESS_CHECK_EXP("successCheckExp"),
    NODE_COMMENT("comment"),
    NODE_WAIT("wait"),
    NODE_OUTGOING_EDGES("outgoingEdges"),
    EDGE_ID("id"),
    EDGE_DESTINATION("destination"),
    EDGE_ON_WALK("onWalk"),
    EDGE_COMMENT("comment"),
    EDGE_REG_EXP("regExp"),
    EDGE_CHECK_EXP("checkExp"),
    EDGE_TRIGGER_ALWAYS("triggerAlways"),
    EDGE_TRIGGER_ON_EOF("triggerOnEof"),
    EDGE_CHANNEL("channel"),
    EDGE_REQUIRED_CONDITIONS("requiredConditions"),
    EDGE_REQUIRED_CONDITIONS_ONE("one"),
    EDGE_REQUIRED_CONDITIONS_ALL("all"),
    EDGE_TIME_INTERVAL_SINCE_LAST_MICROTRANSITION("timeIntervalSinceLastMicrotransition"),
    EDGE_TIME_INTERVAL_SINCE_LAST_TRANSITION("timeIntervalSinceLastTransition"),
    EDGE_TIME_INTERVAL_SINCE_AUTOMATON_START("timeIntervalSinceAutomatonStart"),
    EDGE_TIME_INTERVAL_FOR_EVENT("timeIntervalForEvent"),
    TIME_INTERVAL_MIN("min"),
    TIME_INTERVAL_MAX("max"),
    TIME_INTERVAL_DURATION_IS_INCLUSIVE("isInclusive"),
    TIME_INTERVAL_DURATION_VALUE("value"),
    TIME_INTERVAL_DURATION_UNIT("unit");

    private String keyName;

    private JsonKey(final String aKeyName)
    {
        this.keyName = aKeyName;
    }

    String getKey()
    {
        return this.keyName;
    }
}
