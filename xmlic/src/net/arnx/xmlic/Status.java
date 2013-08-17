package net.arnx.xmlic;

public interface Status {
	public boolean isFirst();
	public boolean isLast();
	public int getIndex();
	public void cancel() throws RuntimeException;
	
	static class StatusImpl implements Status {
		static final RuntimeException CANCEL = new RuntimeException();
		
		boolean first;
		boolean last;
		int index = -1;
		
		void next(int last) {
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
		
		static boolean isCancelException(RuntimeException e) {
			Throwable current = e;
			do {
				if (current == CANCEL) {
					return true; 
				}
			} while ((current = current.getCause()) != null);
			
			return false;
		}
	}
}
