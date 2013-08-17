package net.arnx.xmlic.internal.util;

import net.arnx.xmlic.Status;

public class StatusImpl implements Status {
	private static final RuntimeException CANCEL = new RuntimeException();
	
	private boolean first;
	private boolean last;
	private int index = -1;
	
	public void next(int last) {
		this.index++;
		this.first = (index == 0);
		this.last = (index == last);
	}
	
	@Override
	public boolean isFirst() {
		return first;
	}

	@Override
	public boolean isLast() {
		return last;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void cancel() throws RuntimeException {
		throw CANCEL;
	}
	
	public static boolean isCancelException(RuntimeException e) {
		Throwable current = e;
		do {
			if (current == CANCEL) {
				return true; 
			}
		} while ((current = current.getCause()) != null);
		
		return false;
	}
}
