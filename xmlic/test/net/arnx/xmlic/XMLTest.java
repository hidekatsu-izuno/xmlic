package net.arnx.xmlic;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;

import org.junit.Test;

public class XMLTest {

	@Test
	public void testXML() {
		assertEquals("", new XML().toString());
		assertEquals("<div>test</div>", new XML().doc().append("<div>test</div>").toString());
	}
	
	@Test
	public void testLoad() throws IOException, URISyntaxException {
		String expected = "<html:body xmlns:html=\"http://www.w3.org/1999/xhtml\" xmlns:svg=\"http://www.w3.org/2000/svg\">\ntop\n<html:div class=\"s1\">\n\t<html:ul class=\"s11\">\n\t\t<html:li>t1</html:li>\n\t\t<html:li>t2</html:li>\n\t\t<html:li>t3</html:li>\n\t</html:ul>\n\t<html:ul class=\"s12\">\n\t\t<html:li>t4</html:li>\n\t\t<html:li>t5</html:li>\n\t\t<html:li>t6</html:li>\n\t</html:ul>\n</html:div>\nmiddle\n<html:div class=\"s2\">\n\tprefix\n\t<html:ul svg:class=\"s21\">\n\t\t<html:li>t7</html:li>\n\t\t<html:li>t8</html:li>\n\t\t<html:li>t9</html:li>\n\t</html:ul>\n\tsuffix\n</html:div>\nbottom\n</html:body>";
		assertEquals(expected, XML.load(new File("test/net/arnx/xmlic/test_ns.xml")).toString());
		assertEquals(expected, XML.load(new File("test/net/arnx/xmlic/test_ns.xml").toURI()).toString());
		assertEquals(expected, XML.load(getClass().getResource("test_ns.xml").toURI()).toString());
		assertEquals(expected, XML.load(getClass().getResource("test_ns.xml")).toString());
		assertEquals(expected, XML.load(getClass().getResourceAsStream("test_ns.xml")).toString());
		assertEquals(expected, XML.load(new InputStreamReader(getClass().getResourceAsStream("test_ns.xml"), "UTF-8")).toString());
	}
	
	@Test
	public void testExtendedFunction() throws IOException {
		XML xml = XML.load(getClass().getResource("test_ns.xml"));
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.evaluate("document('test.xml')", Nodes.class).toString());
		assertEquals("<html:ul xmlns:html=\"http://www.w3.org/1999/xhtml\" class=\"s11\">\n\t\t<html:li>t1</html:li>\n\t\t<html:li>t2</html:li>\n\t\t<html:li>t3</html:li>\n\t</html:ul><html:ul xmlns:html=\"http://www.w3.org/1999/xhtml\" class=\"s12\">\n\t\t<html:li>t4</html:li>\n\t\t<html:li>t5</html:li>\n\t\t<html:li>t6</html:li>\n\t</html:ul>", xml.find(".//html:div").find("./html:ul[current()/@class='s1']").toString());

		xml.addKey("class-id", "*[@class]", "@class");
		assertEquals("<html:ul xmlns:html=\"http://www.w3.org/1999/xhtml\" class=\"s11\">\n\t\t<html:li>t1</html:li>\n\t\t<html:li>t2</html:li>\n\t\t<html:li>t3</html:li>\n\t</html:ul>", xml.find("key('class-id', 's11')").toString());
	}
	
	@Test
	public void testWriteTo() throws IOException {
		XML xml = XML.load(getClass().getResource("test_ns.xml"));
		
		File tmpfile;
		
		tmpfile= File.createTempFile(getClass().getSimpleName(), ".tmp");
		xml.writeTo(tmpfile);
		assertEquals(xml.toString(), XML.load(tmpfile).toString());
		tmpfile.delete();
		
		tmpfile = File.createTempFile(getClass().getSimpleName(), ".tmp");
		xml.writeTo(new FileOutputStream(tmpfile));
		assertEquals(xml.toString(), XML.load(tmpfile).toString());
		tmpfile.delete();
		
		tmpfile = File.createTempFile(getClass().getSimpleName(), ".tmp");
		xml.writeTo(new OutputStreamWriter(new FileOutputStream(tmpfile), "UTF-8"));
		assertEquals(xml.toString(), XML.load(tmpfile).toString());
		tmpfile.delete();		
	}
}
