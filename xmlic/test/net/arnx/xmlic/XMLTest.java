package net.arnx.xmlic;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class XMLTest {

	@Test
	public void testXML() {
		assertEquals("", new XML().toString());
	}
	
	@Test
	public void testRemoveNamespace() throws IOException {
		XML xml = XML.load(getClass().getResource("test_ns.xml"));
		assertEquals("<html:body xmlns:html=\"http://www.w3.org/1999/xhtml\">\ntop\n<html:div class=\"s1\">\n\t<html:ul class=\"s11\">\n\t\t<html:li>t1</html:li>\n\t\t<html:li>t2</html:li>\n\t\t<html:li>t3</html:li>\n\t</html:ul>\n\t<html:ul class=\"s12\">\n\t\t<html:li>t4</html:li>\n\t\t<html:li>t5</html:li>\n\t\t<html:li>t6</html:li>\n\t</html:ul>\n</html:div>\nmiddle\n<html:div class=\"s2\">\n\tprefix\n\t<html:ul class=\"s21\">\n\t\t<html:li>t7</html:li>\n\t\t<html:li>t8</html:li>\n\t\t<html:li>t9</html:li>\n\t</html:ul>\n\tsuffix\n</html:div>\nbottom\n</html:body>", xml.removeNamespace("http://www.w3.org/2000/svg").toString());
		assertEquals("<body>\ntop\n<div class=\"s1\">\n\t<ul class=\"s11\">\n\t\t<li>t1</li>\n\t\t<li>t2</li>\n\t\t<li>t3</li>\n\t</ul>\n\t<ul class=\"s12\">\n\t\t<li>t4</li>\n\t\t<li>t5</li>\n\t\t<li>t6</li>\n\t</ul>\n</div>\nmiddle\n<div class=\"s2\">\n\tprefix\n\t<ul class=\"s21\">\n\t\t<li>t7</li>\n\t\t<li>t8</li>\n\t\t<li>t9</li>\n\t</ul>\n\tsuffix\n</div>\nbottom\n</body>", xml.removeNamespace("http://www.w3.org/1999/xhtml").toString());
	}
}
