package net.arnx.xmlic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.XMLConstants;

import net.arnx.xmlic.internal.org.jaxen.NamespaceContext;

class NamespaceContextImpl implements NamespaceContext, Iterable<Map.Entry<String, List<String>>>, Serializable {
	private static final long serialVersionUID = 1L;
	
	private Map<String, List<String>> map = Collections.synchronizedMap(new LinkedHashMap<String, List<String>>());
	
	public NamespaceContextImpl() {
	}
	
	@Override
	public Iterator<Entry<String, List<String>>> iterator() {
		return Collections.unmodifiableMap(map).entrySet().iterator();
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
	public String translateNamespacePrefixToUri(String prefix) {
		return getNamespaceURI(prefix);
	}
	
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
	
	public Iterator<?> getPrefixes(String namespaceURI) {
		if (namespaceURI == null) {
			throw new IllegalArgumentException("namespaceURI is null.");
		} else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
			return Arrays.asList(XMLConstants.DEFAULT_NS_PREFIX).iterator();
		} else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
			return Arrays.asList(XMLConstants.XML_NS_PREFIX).iterator();
		} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
			return Arrays.asList(XMLConstants.XMLNS_ATTRIBUTE).iterator();
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
}
