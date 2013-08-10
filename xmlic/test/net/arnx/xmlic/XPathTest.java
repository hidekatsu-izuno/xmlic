package net.arnx.xmlic;

import static org.junit.Assert.assertEquals;
import net.arnx.xmlic.internal.util.XmlicContext;
import net.arnx.xmlic.internal.util.XmlicXPath;

import org.junit.Test;

public class XPathTest {
	@Test
	public void testXmlicXPath() throws Exception {
		XmlicContext xcontext = new XmlicContext();
		assertEquals(new XmlicXPath(xcontext, ".//a", false).toString(), new XmlicXPath(xcontext, ".//a", true).toString());
		assertEquals(new XmlicXPath(xcontext, "descendant-or-self::node()/a[@name]", false).toString(), new XmlicXPath(xcontext, "a[@name]", true).toString());
		assertEquals(new XmlicXPath(xcontext, "descendant-or-self::node()/child::node()[@name='test']", false).toString(), new XmlicXPath(xcontext, "@name='test'", true).toString());
		assertEquals(new XmlicXPath(xcontext, "parent::a", false).toString(), new XmlicXPath(xcontext, "parent::a", true).toString());
		assertEquals(new XmlicXPath(xcontext, "descendant-or-self::node()/attribute::*", false).toString(), new XmlicXPath(xcontext, "@*", true).toString());
	}
}
