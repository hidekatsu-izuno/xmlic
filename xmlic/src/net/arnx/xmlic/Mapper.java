package net.arnx.xmlic;

public interface Mapper<T> {
	public T map(Context<T> context);
}
