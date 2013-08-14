package net.arnx.xmlic;

public interface State {
	public boolean isFirst();
	public boolean isLast();
	public Nodes getSource();
	public int getIndex();
	public RuntimeException cancel();
}
