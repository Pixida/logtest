/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StringToGenericTimeIntervalConverter
{
    private static final Pattern INTERVAL_PATTERN = Pattern.compile("^\\s*([\\(\\[])\\s*(.+?)\\s*;\\s*(.+?)\\s*([\\)\\]])\\s*$");
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("^(\\s*(=|>=|<=|<|>))?\\s*(.+?)\\s*$");
    private static final Pattern VALUE_AND_UNIT_PATTERN = Pattern.compile("^\\s*(.+?)(\\s*([a-z]+))?\\s*$");
    private static final Map<String, TimeUnit> STR_TO_UNIT_MAP = new HashMap<>();

    static ITimeInterval convertStringToTimestamp(final String str)
    {
        final Matcher intervalMatcher = INTERVAL_PATTERN.matcher(str);

        if (intervalMatcher.matches())
        {
            return parseDefinitionAsInterval(intervalMatcher);
        }
        else
        {
            final Matcher operatorMatcher = OPERATOR_PATTERN.matcher(str);
            if (operatorMatcher.matches())
            {
                return parseDefinitionWithOperators(operatorMatcher);
            }
            else
            {
                throw new AutomatonLoadingException("Invalid time interval value: " + str);
            }
        }
    }

    private static ITimeInterval parseDefinitionWithOperators(final Matcher operatorMatcher)
    {
        GenericDuration t0 = null;
        GenericDuration t1 = null;

        final String operatorStr = operatorMatcher.group(1) != null ? operatorMatcher.group(1 + 1) : ">=";
        if (operatorStr.equals("="))
        {
            t0 = new GenericDuration();
            t1 = new GenericDuration();
            t0.setInclusive(true);
            t1.setInclusive(true);
            extractValueAndUnit(operatorMatcher.group(1 + 1 + 1), t0);
            t1.setValue(t0.getValue());
            t1.setUnit(t0.getUnit());
        }
        else
        {
            GenericDuration valueDest;
            if (operatorStr.charAt(0) == '>')
            {
                t0 = new GenericDuration();
                valueDest = t0;
            }
            else if (operatorStr.charAt(0) == '<')
            {
                t1 = new GenericDuration();
                valueDest = t1;
            }
            else
            {
                throw new RuntimeException("Invernal error; Unhandled operator: " + operatorStr);
            }
            valueDest.setInclusive(operatorStr.length() > 1);
            extractValueAndUnit(operatorMatcher.group(1 + 1 + 1), valueDest);
        }

        final GenericTimeInterval result = new GenericTimeInterval();
        result.setMin(t0);
        result.setMax(t1);
        return result;
    }

    private static ITimeInterval parseDefinitionAsInterval(final Matcher intervalMatcher)
    {
        final GenericDuration t0 = new GenericDuration();
        final GenericDuration t1 = new GenericDuration();
        t0.setInclusive(intervalMatcher.group(1).equals("["));
        extractValueAndUnit(intervalMatcher.group(1 + 1), t0);
        extractValueAndUnit(intervalMatcher.group(1 + 1 + 1), t1);
        t1.setInclusive(intervalMatcher.group(1 + 1 + 1 + 1).equals("]"));
        final GenericTimeInterval result = new GenericTimeInterval();
        result.setMin(t0);
        result.setMax(t1);
        return result;
    }

    static TimeUnit parseUnit(final String unitStr)
    {
        final TimeUnit result = getStrToUnitMap().get(unitStr);
        if (result == null)
        {
            throw new AutomatonLoadingException("Invalid unit: " + unitStr);
        }
        return result;
    }

    private static void extractValueAndUnit(final String valueAndUnit, final GenericDuration dest)
    {
        final Matcher valueAndUnitMatcher = VALUE_AND_UNIT_PATTERN.matcher(valueAndUnit);
        if (valueAndUnitMatcher.matches())
        {
            dest.setValue(valueAndUnitMatcher.group(1));
            final String unitStr = valueAndUnitMatcher.group(1 + 1 + 1);
            if (unitStr == null)
            {
                dest.setUnit(TimeUnit.MILLISECONDS);
            }
            else
            {
                dest.setUnit(parseUnit(unitStr));
            }
        }
        else
        {
            throw new AutomatonLoadingException("Invalid value and unit definition: " + valueAndUnit);
        }
    }

    private static Map<String, TimeUnit> getStrToUnitMap()
    {
        for (final JsonTimeUnit unit : JsonTimeUnit.values())
        {
            STR_TO_UNIT_MAP.put(unit.getName(), unit.getTimeUnit());
        }
        return STR_TO_UNIT_MAP;
    }
}
