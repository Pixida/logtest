/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.reporting;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.Job;
import de.pixida.logtest.processing.LogSink;

public class JUnitStyleXmlReportGenerator implements IReportGenerator
{
    private static final Logger LOG = LoggerFactory.getLogger(JUnitStyleXmlReportGenerator.class);

    private final File destFile;
    private final long totalTimeMs;
    private final Map<Job, List<Triple<LogSink, EvaluationResult, Long>>> results = new LinkedHashMap<>(); // Preserve ordering

    public JUnitStyleXmlReportGenerator(final File aDestFile, final long aTotalTimeMs)
    {
        Validate.notNull(aDestFile, "Destination file must be set!");
        Validate.isTrue(aTotalTimeMs >= 0L, "Total time cannot be negative.");
        this.destFile = aDestFile;
        this.totalTimeMs = aTotalTimeMs;
    }

    @Override
    public void start()
    {
    }

    @Override
    public void pushExecution(final Job job, final LogSink sink, final EvaluationResult evaluationResult, final long timeMs)
    {
        List<Triple<LogSink, EvaluationResult, Long>> executions = this.results.get(job);
        if (executions == null)
        {
            executions = new ArrayList<>();
            this.results.put(job, executions);
        }
        executions.add(Triple.of(sink, evaluationResult, timeMs));
    }

    @Override
    public void finish()
    {
        try
        {
            // Create document
            final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Create root element
            final Document doc = docBuilder.newDocument();
            final Element rootElement = doc.createElement("testsuite");
            rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            rootElement.setAttribute("xsi:schemaLocation",
                "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd");
            rootElement.setAttribute("name", "LogFileAnalysisTestsuite");
            final double msToSec = 1000.0;
            rootElement.setAttribute("time", String.valueOf(this.totalTimeMs / msToSec));
            final long numTests = this.results.size();
            final long numSuccesses = this.results.values().stream()
                .mapToLong(result -> result.stream().filter(r -> r.getMiddle().isSuccess()).count()).sum();
            final long numFailures = this.results.values().stream()
                .mapToLong(result -> result.stream().filter(r -> r.getMiddle().isFailure()).count()).sum();
            final long numSkipped = 0L;
            final long numErrors = numTests - numSuccesses - numFailures;
            rootElement.setAttribute("tests", String.valueOf(numTests));
            rootElement.setAttribute("errors", String.valueOf(numErrors));
            rootElement.setAttribute("skipped", String.valueOf(numSkipped));
            rootElement.setAttribute("failures", String.valueOf(numFailures));
            doc.appendChild(rootElement);

            // Create payload
            this.addProperties(doc, rootElement);
            this.addTestCases(doc, rootElement);

            // Dump results into file
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            final DOMSource source = new DOMSource(doc);
            final File parentFile = this.destFile.getParentFile();
            if (parentFile != null)
            {
                parentFile.mkdirs(); // If fails, will be caught by the following operations
            }
            final StreamResult result = new StreamResult(this.destFile);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        }
        catch (final DOMException | TransformerException | ParserConfigurationException e)
        {
            LOG.error("Failed to write results", e);
        }
    }

    private void addProperties(final Document doc, final Element rootElement)
    {
        final Element properties = doc.createElement("properties");
        for (final Object property : System.getProperties().keySet())
        {
            final String key = property.toString();
            final String value = System.getProperty(key);
            final Element element = doc.createElement("property");
            element.setAttribute("name", key);
            element.setAttribute("value", value);
            properties.appendChild(element);
        }
        rootElement.appendChild(properties);
    }

    private void addTestCases(final Document doc, final Element rootElement)
    {
        for (final Entry<Job, List<Triple<LogSink, EvaluationResult, Long>>> resultSet : this.results.entrySet())
        {
            // The class name usually includes its packet name which is displayed as tree in Jenkins.
            // This might lead to the following tree hierarchy: "some-log" -> "txt" -> ..results..
            //                                                  "some-other-log" -> "txt" -> ..results..
            // Furthermore, we down't know how the log source is represented as human readable string, so it may always contain
            // dots, e.g. "127.0.0.1:8082" for stream sources.
            // For now, replace all dots by a "-", such that the human readable identifier of the log source remains human readable and
            // all information is preserved.
            String className = resultSet.getKey().getLogReader().getDisplayName();
            className = className.replace('.', '-');

            for (final Triple<LogSink, EvaluationResult, Long> result : resultSet.getValue())
            {
                final String testcaseName = result.getLeft().getDisplayName();
                final Element testcase = doc.createElement("testcase");

                testcase.setAttribute("classname", className);
                testcase.setAttribute("name", testcaseName);
                // Actually, set the job time, not the test time (tests run in parallel). The attribute is mandatory.
                final long msToSec = 1000;
                testcase.setAttribute("time", String.valueOf((double) result.getRight() / msToSec));
                if (!result.getMiddle().isSuccess())
                {
                    final Element error = doc.createElement(result.getMiddle().isError() ? "error" : "failure");
                    assert result.getMiddle().getMessage() != null;
                    error.setAttribute("type", result.getMiddle().getResult().toString());
                    error.setAttribute("message", result.getMiddle().getMessage());
                    testcase.appendChild(error);
                }
                rootElement.appendChild(testcase);
            }
        }
    }
}
