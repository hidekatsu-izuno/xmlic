package net.arnx.xmlic;

public interface Visitor<T> {
	public boolean visit(int index, T node);
}
