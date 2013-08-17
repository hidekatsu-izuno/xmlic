package net.arnx.xmlic;

import java.lang.reflect.Method;

public interface Status {
	public boolean isFirst();
	public boolean isLast();
	public int getIndex();
	public void cancel() throws RuntimeException;
	
	static class StatusImpl implements Status {
		static final RuntimeException CANCEL = new RuntimeException();
		
		private static final Class<?> RHINO_WRAPPED_EXCEPTION;
		private static final Method RHINO_UNWRAP;
		
		static {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class<?> cls = null;
			Method m = null;
			try {
				cls = Class.forName("org.mozilla.javascript.WrappedException", true, cl);
				m = cls.getMethod("unwrap");
			} catch (Throwable t) {
			}
			RHINO_WRAPPED_EXCEPTION = cls;
			RHINO_UNWRAP = m;
		}
		
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
		
		static RuntimeException unwrap(RuntimeException e) {
			if (RHINO_WRAPPED_EXCEPTION != null 
					&& RHINO_UNWRAP != null
					&& RHINO_WRAPPED_EXCEPTION.isAssignableFrom(e.getClass())) {
				
				try {
					Object o = RHINO_UNWRAP.invoke(e);
					if (o instanceof Error) {
						throw (Error)o;
					} else if (o instanceof RuntimeException) {
						return (RuntimeException)o;
					}
				} catch (Exception e1) {
					// no handle
				}
			}
			return e;
		}
	}
}
