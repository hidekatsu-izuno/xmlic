package net.arnx.xmlic;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class XSLTTest {

	@Test
	public void testLoad() throws IOException {
		XSLT xslt = XSLT.load(getClass().getResource("test.xsl"));
		assertNotNull(xslt);
		
		try {
			xslt = XSLT.load(getClass().getResource("test_error.xsl"));
			fail();
		} catch (XSLTException e) {
			assertNotNull(e);
		}
	}
	
	@Test
	public void testStylesheet() throws IOException {
		assertNull(XML.load(getClass().getResource("test.xml")).stylesheet());
		assertNotNull(XML.load(getClass().getResource("test_xslt.xml")).stylesheet());
	}
}
