package net.arnx.xmlic;

public interface Visitor<T> {
	public void visit(Context<T> context);
}
