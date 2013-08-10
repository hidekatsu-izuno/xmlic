package net.arnx.xmlic;

public interface Visitor<T> {
	public static final RuntimeException BREAK = new RuntimeException("BREAK");
	
	public void visit(int index, T current);
}
