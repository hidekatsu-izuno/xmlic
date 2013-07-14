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
		assertEquals("", xml.removeNamespace("http://www.w3.org/2000/svg").toString());
		assertEquals("", xml.removeNamespace("http://www.w3.org/1999/xhtml").toString());
	}

}
