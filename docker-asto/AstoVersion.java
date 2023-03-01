/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Extracts version of artifact 'com.artipie:asto-core' from pom.xml.
 * @since 0.28.1
 */
public class AstoVersion {
    public static void main(final String[] args) throws IOException, ParserConfigurationException,
        SAXException, XPathExpressionException {
        if (args.length == 0) {
            System.err.println("Usage: <PATH to pom.xml>");
            System.exit(1);
        }
        try (FileInputStream fis = new FileInputStream(Paths.get(args[0]).toFile())) {
            final DocumentBuilder builder = DocumentBuilderFactory
                .newInstance().newDocumentBuilder();
            final Document document = builder.parse(fis);
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final String expression = ".//dependency/version[../groupId[text() = 'com.artipie'] and ../artifactId[text() = 'asto-core']]";
            final String value = (String) xpath.compile(expression)
                .evaluate(document, XPathConstants.STRING);
            System.out.print(value);
        }
    }
}
