package net.arnx.xmlic;

public interface Context<T> {
	public boolean isFirst();
	public boolean isLast();
	public Nodes getSource();
	public int getIndex();
	public T getItem();
	public RuntimeException cancel();
}
