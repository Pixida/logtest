/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.reporting;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.EvaluationResult.Result;
import de.pixida.logtest.processing.Job;
import de.pixida.logtest.processing.JobExecutorTest;

public class XUnitReportGeneratorTest
{
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    public XUnitReportGeneratorTest()
    {
        // Empty constructor needed by checkstyle
    }

    @Test
    public void testReportGeneratesValidOutput() throws IOException
    {
        final long totalTimeMs = 1000;
        final File tmpFile = this.tempFolder.newFile();
        final XUnitReportGenerator generator = new XUnitReportGenerator(tmpFile, totalTimeMs);
        final List<Job> jobs = JobExecutorTest.createSomeStupidJobs();
        final EvaluationResult[] possibleResults = new EvaluationResult[Result.values().length];
        possibleResults[0] = new EvaluationResult(Result.SUCCESS, null);
        possibleResults[1] = new EvaluationResult(Result.FAILURE, "some failure");
        possibleResults[1 + 1] = new EvaluationResult(Result.INTERNAL_ERROR, "internal error");
        possibleResults[1 + 1 + 1] = new EvaluationResult(Result.AUTOMATON_DEFECT, "automaton is defect");
        // We must have enough tests to iterate through all result variants. I.e. this is an assertion to test if this test is meaningful.
        Assert.assertTrue(possibleResults.length <= jobs.stream().mapToLong(job -> job.getSinks().size()).sum());
        for (final EvaluationResult possibleResult : possibleResults)
        {
            if (possibleResult == null)
            {
                Assert.fail("Possible results must have a result for all result types!");
            }
        }
        int totalI = 0;
        generator.start();
        for (int i = 0; i < jobs.size(); i++)
        {
            for (int j = 0; j < jobs.get(i).getSinks().size(); j++)
            {
                generator.pushExecution(jobs.get(i), jobs.get(i).getSinks().get(j), possibleResults[totalI % possibleResults.length], 1L);
                totalI++;
            }
        }
        generator.finish();
        this.validateResult(tmpFile);
    }

    private void validateResult(final File file) throws IOException
    {
        final Source xmlFile = new StreamSource(file);
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try
        {
            final Schema schema = schemaFactory.newSchema(new File(this.getClass().getResource("surefire-test-report.xsd").getFile()));
            final Validator validator = schema.newValidator();
            validator.validate(xmlFile);
        }
        catch (final SAXException e)
        {
            Assert.fail("Report generator generated invalid XML: " + e.getMessage());
        }
    }
}
