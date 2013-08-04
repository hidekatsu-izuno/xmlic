package net.arnx.xmlic;

public class XPathSyntaxException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final String xpath;
	private final int position;
	
	public XPathSyntaxException(String xpath, int pos, String message, Throwable t) {
		super(message, t);
		this.xpath = xpath;
		this.position = pos;
	}
	
	public String getXPath() {
		return xpath;
	}
	
	public int getPosition() {
		return position;
	}
}
