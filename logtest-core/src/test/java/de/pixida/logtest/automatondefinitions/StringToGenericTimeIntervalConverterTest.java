/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.automatondefinitions;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class StringToGenericTimeIntervalConverterTest
{
    private ITimeInterval generatedInterval;

    public StringToGenericTimeIntervalConverterTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testDefinitionViaSingleNumber()
    {
        this.parse("5");
        this.assumeMin("5", TimeUnit.MILLISECONDS, true);
        this.assumeNoMax();
    }

    @Test
    public void testDefinitionViaSingleNegativeNumber()
    {
        this.parse("-15");
        this.assumeMin("-15", TimeUnit.MILLISECONDS, true);
        this.assumeNoMax();
    }

    @Test
    public void testDefinitionViaSingleNumberAndUnit()
    {
        this.parse("-15s");
        this.assumeMin("-15", TimeUnit.SECONDS, true);
        this.assumeNoMax();
    }

    @Test
    public void testDefinitionViaNumberAndGeqOperatorAndUnit()
    {
        this.parse(">=-15s");
        this.assumeMin("-15", TimeUnit.SECONDS, true);
        this.assumeNoMax();
    }

    @Test
    public void testDefinitionViaNumberAndEqOperatorAndUnit()
    {
        this.parse("=-15m");
        this.assumeMin("-15", TimeUnit.MINUTES, true);
        this.assumeMax("-15", TimeUnit.MINUTES, true);
    }

    @Test
    public void testDefinitionViaNumberAndLeqOperatorAndUnit()
    {
        this.parse("<=-15m");
        this.assumeNoMin();
        this.assumeMax("-15", TimeUnit.MINUTES, true);
    }

    @Test
    public void testDefinitionViaNumberAndLtOperatorAndUnit()
    {
        this.parse("<-15m");
        this.assumeNoMin();
        this.assumeMax("-15", TimeUnit.MINUTES, false);
    }

    @Test
    public void testDefinitionViaNumberAndGtOperatorAndUnit()
    {
        this.parse("> 3 d");
        this.assumeMin("3", TimeUnit.DAYS, false);
        this.assumeNoMax();
    }

    @Test
    public void testDefinitionViaNumberAndGtOperator()
    {
        this.parse(" > 3 ");
        this.assumeMin("3", TimeUnit.MILLISECONDS, false);
        this.assumeNoMax();
    }

    @Test
    public void testIntervalWithUnit()
    {
        this.parse("    [   3   s   ;   5   \t   d   )       ");
        this.assumeMin("3", TimeUnit.SECONDS, true);
        this.assumeMax("5", TimeUnit.DAYS, false);
    }

    @Test
    public void testIntervalWithoutUnit()
    {
        this.parse(" ( 5 ;7 ] ");
        this.assumeMin("5", TimeUnit.MILLISECONDS, false);
        this.assumeMax("7", TimeUnit.MILLISECONDS, true);
    }

    private void parse(final String str)
    {
        this.generatedInterval = StringToGenericTimeIntervalConverter.convertStringToTimestamp(str);
    }

    private void assumeMin(final String value, final TimeUnit unit, final boolean isInclusive)
    {
        Assert.assertEquals(value, this.generatedInterval.getMin().getValue());
        Assert.assertEquals(unit, this.generatedInterval.getMin().getUnit());
        Assert.assertEquals(isInclusive, this.generatedInterval.getMin().isInclusive());
    }

    private void assumeMax(final String value, final TimeUnit unit, final boolean isInclusive)
    {
        Assert.assertEquals(value, this.generatedInterval.getMax().getValue());
        Assert.assertEquals(unit, this.generatedInterval.getMax().getUnit());
        Assert.assertEquals(isInclusive, this.generatedInterval.getMax().isInclusive());
    }

    private void assumeNoMin()
    {
        Assert.assertNull(this.generatedInterval.getMin());
    }

    private void assumeNoMax()
    {
        Assert.assertNull(this.generatedInterval.getMax());
    }
}
