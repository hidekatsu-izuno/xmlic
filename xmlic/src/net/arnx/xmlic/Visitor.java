package net.arnx.xmlic;

public interface Visitor<T> {
	public boolean visit(int i, T value);
}
