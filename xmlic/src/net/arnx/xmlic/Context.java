package net.arnx.xmlic;

public interface Context {
	public boolean isFirst();
	public boolean isLast();
	public Nodes getSource();
	public int getIndex();
	public RuntimeException cancel();
}
