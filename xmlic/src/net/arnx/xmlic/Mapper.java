package net.arnx.xmlic;

public interface Mapper<S, R> {
	public R map(S value, Status status);
}
