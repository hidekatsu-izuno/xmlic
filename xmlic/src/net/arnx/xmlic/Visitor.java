package net.arnx.xmlic;

public interface Visitor<T> {
	public void visit(T current, Status status);
}
