package net.arnx.xmlic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

class NamespaceContextImpl implements NamespaceContext, Serializable {
	private static final long serialVersionUID = 1L;
	
	private Map<String, List<String>> map = Collections.synchronizedMap(new LinkedHashMap<String, List<String>>());
	
	public NamespaceContextImpl() {
	}
	
	public NamespaceContextImpl(Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			this.map.put(entry.getKey(), list(entry.getValue()));
		}
	}
	
	public void addNamespace(String prefix, String namespaceURI) {
		synchronized (this) {
			List<String> list = map.get(prefix);
			if (list == null) {
				list = new ArrayList<String>(1);
				list.add(namespaceURI);
				map.put(prefix, list);
			} else if (!list.contains(namespaceURI)) {
				list.add(namespaceURI);
			}
		}
	}
	
	@Override
	public String getNamespaceURI(String prefix) {
		if (prefix == null) {
			throw new IllegalArgumentException("prefix is null.");
		} else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			List<String> list = map.get(prefix);
			return (list != null && !list.isEmpty()) ? list.get(0) : XMLConstants.NULL_NS_URI;
		} else if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		} else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		} else {
			List<String> list = map.get(prefix);
			return (list != null && !list.isEmpty()) ? list.get(0) : null;
		}
	}
	
	@Override
	public String getPrefix(String namespaceURI) {
		if (namespaceURI == null) {
			throw new IllegalArgumentException("namespaceURI is null.");
		} else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
			return XMLConstants.DEFAULT_NS_PREFIX;
		} else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
			return XMLConstants.XML_NS_PREFIX;
		} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
			return XMLConstants.XMLNS_ATTRIBUTE;
		} else {
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				if (entry.getValue().contains(namespaceURI)) {
					return entry.getKey();
				}
			}
			return null;
		}
	}
	
	@Override
	public Iterator<?> getPrefixes(String namespaceURI) {
		if (namespaceURI == null) {
			throw new IllegalArgumentException("namespaceURI is null.");
		} else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
			return list(XMLConstants.DEFAULT_NS_PREFIX).iterator();
		} else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
			return list(XMLConstants.XML_NS_PREFIX).iterator();
		} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
			return list(XMLConstants.XMLNS_ATTRIBUTE).iterator();
		} else {
			Set<String> result = new LinkedHashSet<String>();
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				if (entry.getValue().contains(namespaceURI)) {
					result.add(entry.getKey());
				}
			}
			return Collections.unmodifiableSet(result).iterator();
		}
	}
	
	private static List<String> list(String text) {
		List<String> list = new ArrayList<String>(1);
		list.add(text);
		return list;
	}
}
