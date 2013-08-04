package net.arnx.xmlic.internal.function;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import net.arnx.xmlic.internal.org.jaxen.Context;
import net.arnx.xmlic.internal.org.jaxen.Function;
import net.arnx.xmlic.internal.org.jaxen.FunctionCallException;
import net.arnx.xmlic.internal.org.jaxen.Navigator;
import net.arnx.xmlic.internal.org.jaxen.XPath;
import net.arnx.xmlic.internal.org.jaxen.function.StringFunction;
import net.arnx.xmlic.internal.org.jaxen.saxpath.SAXPathException;
import net.arnx.xmlic.internal.util.XMLContext;
import net.arnx.xmlic.internal.util.XMLContext.Key;

public class KeyFunction implements Function {
	@SuppressWarnings("rawtypes")
	@Override
	public Object call(Context context, List args) throws FunctionCallException {
		if (args == null || args.size() != 2) {
			throw new FunctionCallException("key() requires two argument.");
		} else if (!(args.get(0) instanceof String)) {
			throw new FunctionCallException("invalid argument: key(string, node)");
		}
		
		Navigator nav = context.getNavigator();
		XMLContext xcontext = (XMLContext)context.getContextSupport().getVariableContext();
		Node current = xcontext.getCurrentNode();
		Object doc = nav.getDocumentNode(current);
		
		Key key = xcontext.getKey((String)args.get(0));
		String value = StringFunction.evaluate(args.get(1), nav);
		if (value == null) {
			return Collections.EMPTY_LIST;
		}
		
		try {
			List result = nav.parseXPath(".//" + key.match).selectNodes(doc);
			XPath use = nav.parseXPath(key.use);
			Iterator i = result.iterator();
			while (i.hasNext()) {
				Object node = i.next();
				if (!value.equals(use.stringValueOf(node))) {
					i.remove();
				}
			}
			return result;
		} catch (SAXPathException e) {
			throw new FunctionCallException(e);
		}
	}
}
