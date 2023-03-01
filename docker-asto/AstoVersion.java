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
 * Extracts version of artifact 'com.artipie:asto-core' from pom.xml
 */
public class AstoVersion {
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        if (args.length == 0) {
            System.err.println("Usage: <PATH to pom.xml>");
            System.exit(1);
        }
        FileInputStream fis = new FileInputStream(Paths.get(args[0]).toFile());
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(fis);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = ".//dependency/version[../groupId[text() = 'com.artipie'] and ../artifactId[text() = 'asto-core']]";
        final String value = (String) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.STRING);
        System.out.print(value);
        fis.close();
    }
}
