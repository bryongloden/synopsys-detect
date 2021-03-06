package com.synopsys.integration.detect.tool.bazel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XPathParserTest {

    @Test
    public void testSimpleXml() throws Exception {
        final String xmlFilePath = "src/test/resources/bazel/sample1.xml";
        List<String> externalIds = parseXmlFile(xmlFilePath);
        assertEquals(1, externalIds.size());
        assertTrue(externalIds.contains("com.google.guava:guava:18.0"));
    }

    @Test
    public void testMoreComplexXml() throws Exception {
        final String xmlFilePath = "src/test/resources/bazel/sample2.xml";
        List<String> externalIds = parseXmlFile(xmlFilePath);
        assertEquals(2, externalIds.size());
        assertTrue(externalIds.contains("org.apache.commons:commons-io:1.3.2"));
        assertTrue(externalIds.contains("com.google.guava:guava:18.0"));
    }

    private List<String> parseXmlFile(final String xmlFilePath) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        final String xml = FileUtils.readFileToString(new File(xmlFilePath), StandardCharsets.UTF_8);
        XPathParser parser = new XPathParser();
        List<String> externalIds = parser.parseAttributeValuesWithGivenXPathQuery(xml,"/query/rule/string[@name='artifact']", "value");
        return externalIds;
    }

}
