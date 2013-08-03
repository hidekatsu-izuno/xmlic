package net.arnx.xmlic;

public class XPathSyntaxException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final String xpath;
	private final int position;
	
	XPathSyntaxException(net.arnx.xmlic.internal.org.jaxen.XPathSyntaxException e) {
		super(e.getMultilineMessage(), e);
		this.xpath = e.getXPath();
		this.position = e.getPosition();
	}
	
	public String getXPath() {
		return xpath;
	}
	
	public int getPosition() {
		return position;
	}
}
