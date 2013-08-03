package net.arnx.xmlic;

public interface Visitor<T> {
	public Boolean visit(int index, T node);
}
