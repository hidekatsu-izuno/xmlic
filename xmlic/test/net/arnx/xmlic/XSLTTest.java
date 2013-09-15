package net.arnx.xmlic;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class XSLTTest {

	@Test
	public void testLoad() throws IOException {
		XSLT xslt = XSLT.load(getClass().getResource("test.xsl"));
		assertNotNull(xslt);
		
		xslt = XSLT.load(getClass().getResource("test_error.xsl"));
	}

}
