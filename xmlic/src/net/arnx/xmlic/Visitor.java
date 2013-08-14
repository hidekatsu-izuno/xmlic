package net.arnx.xmlic;

public interface Visitor<T> {
	public void visit(T value, State state);
}
