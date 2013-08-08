package net.arnx.xmlic;

import static org.junit.Assert.assertEquals;
import net.arnx.xmlic.internal.util.XMLContext;
import net.arnx.xmlic.internal.util.XmlicXPath;

import org.junit.Test;

public class XPathTest {
	@Test
	public void testXmlicXPath() throws Exception {
		XMLContext xcontext = new XMLContext();
		assertEquals(new XmlicXPath(xcontext, ".//a", false).toString(), new XmlicXPath(xcontext, ".//a", true).toString());
		assertEquals(new XmlicXPath(xcontext, "descendant-or-self::node()/a[@name]", false).toString(), new XmlicXPath(xcontext, "a[@name]", true).toString());
		assertEquals(new XmlicXPath(xcontext, "descendant-or-self::node()/child::node()[@name='test']", false).toString(), new XmlicXPath(xcontext, "@name='test'", true).toString());
	}
}
