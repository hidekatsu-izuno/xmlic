package net.arnx.xmlic;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import javax.xml.XMLConstants;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Nodes extends ArrayList<Node> {
	private static final long serialVersionUID = 1L;
	
	static enum SelectMode {
		FIRST,
		UNTIL,
		ALL
	}
	
	final XML owner;
	final Nodes back;
	
	Nodes(XML owner, Nodes back, int size) {
		super(size);
		this.owner = owner;
		this.back = back;
	}
	
	Nodes(XML owner, Nodes back, Node node) {
		this(owner, null, 1);
		add(node);
	}
	
	Nodes(Nodes back, Node node) {
		this(back.getOwner(), back, 1);
		add(node);
	}
	
	Nodes(Nodes back, Nodes nodes) {
		this(back.getOwner(), back, nodes.size());
		for (Node node : nodes) {
			add(node.cloneNode(true));
		}
	}
	
	public XML getOwner() {
		return owner;
	}
	
	@Override
	public Node get(int index) {
		if (index < -size() || index >= size()) {
			return null;
		}
		return super.get((index < 0) ? size() + index : index);
	}
	
	public String namespace() {
		if (isEmpty()) return null;
		
		Node self = get(0);
		if (self == null) return null;
		
		switch (self.getNodeType()) {
		case Node.ELEMENT_NODE:
			return self.getNamespaceURI();
		case Node.ATTRIBUTE_NODE:
			if (self.getNamespaceURI() == null) {
				Element elem = ((Attr)self).getOwnerElement();
				if (elem != null) {
					return elem.getNamespaceURI();
				} else {
					return null;
				}
			} else {
				return self.getNamespaceURI();
			}
		}
		return null;
	}
	
	public Nodes namespace(String uri) {
		if (uri == null) uri = "";
		
		for (Node self : this) {
			if (self == null) continue;

			String name = self.getLocalName();

			switch (self.getNodeType()) {
			case Node.ELEMENT_NODE:
			case Node.ATTRIBUTE_NODE:
				if (!self.isDefaultNamespace(uri)) {
					String prefix = self.lookupPrefix(uri);
					if (prefix == null) {
						prefix = getOwner().xmlContext.getPrefix(uri);
					}
					if (prefix != null && !XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
						name = prefix + ":" + name;
					}
				}
				getOwner().doc.renameNode(self, uri, name);
				break;
			}
		}
		
		return this;
	}
	
	public Nodes removeNamespace() {
		return namespace(null);
	}
	
	public Nodes removeNamespace(String uri) {
		if (uri == null) uri = XMLConstants.DEFAULT_NS_PREFIX;
		if (XMLConstants.XML_NS_URI.equals(uri)) {
			throw new IllegalArgumentException("XML namespace can't remove.");
		} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(uri)) {
			throw new IllegalArgumentException("XMLNS namespace can't remove.");
		}
		
		for (Node self : this) {
			if (self == null) continue;
			
			switch (self.getNodeType()) {
			case Node.ELEMENT_NODE:
			case Node.ATTRIBUTE_NODE:
				String suri = self.getNamespaceURI();
				if (suri == null) suri = XMLConstants.DEFAULT_NS_PREFIX;
				if (!suri.equals(uri)) continue;
			
				getOwner().doc.renameNode(self, XMLConstants.DEFAULT_NS_PREFIX, self.getLocalName());
			}
		}
		
		return this;
	}
	
	public String prefix() {
		if (isEmpty()) return null;
		
		Node self = get(0);
		if (self == null) return null;
		
		switch(self.getNodeType()) {
		case Node.ELEMENT_NODE:
		case Node.ATTRIBUTE_NODE:
			return self.getPrefix();
		}
		return null;
	}
	
	public Nodes prefix(String prefix) {
		for (Node self : this) {
			if (self == null) continue;
			
			switch (self.getNodeType()) {
			case Node.ELEMENT_NODE:
			case Node.ATTRIBUTE_NODE:
				String namespace = self.getNamespaceURI();
				String name = self.getLocalName();
				if (prefix != null && !prefix.isEmpty()) {
					if (namespace == null) namespace = XMLConstants.NULL_NS_URI;
					name = prefix + ":" + name;
				}
				getOwner().doc.renameNode(self, namespace, name);
				break;
			}
		}
		
		return this;
	}
	
	public String localName() {
		if (isEmpty()) return null;
		
		Node self = get(0);
		if (self == null) return null;
		
		switch(self.getNodeType()) {
		case Node.ELEMENT_NODE:
		case Node.ATTRIBUTE_NODE:
			return self.getLocalName();
		}
		return null;
	}
	
	public Nodes localName(String localName) {
		for (Node self : this) {
			if (self == null) continue;
			
			switch (self.getNodeType()) {
			case Node.ELEMENT_NODE:
			case Node.ATTRIBUTE_NODE:
				String name = localName;
				if (self.getPrefix() != null && !self.getPrefix().isEmpty()) {
					name = self.getPrefix() + ":" + name;
				}
				getOwner().doc.renameNode(self, self.getNamespaceURI(), name);
				break;
			}
		}
		
		return this;
	}
	
	public String name() {
		if (isEmpty()) return null;
		
		Node self = get(0);
		if (self == null) return null;
		
		switch(self.getNodeType()) {
		case Node.ELEMENT_NODE:
		case Node.ATTRIBUTE_NODE:
		case Node.DOCUMENT_TYPE_NODE:
		case Node.ENTITY_NODE:
		case Node.ENTITY_REFERENCE_NODE:
		case Node.NOTATION_NODE:
		case Node.PROCESSING_INSTRUCTION_NODE:
			return self.getNodeName();
		}
		return null;
	}
	
	public Nodes name(String name) {
		if (name == null) throw new IllegalArgumentException("name is null");
		if (isEmpty()) return this;
		
		String uri = null;
		String prefix = null;
		String localName;
		
		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			prefix = name.substring(0, index);
			uri = getOwner().xmlContext.getNamespaceURI(prefix);
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return null;
		if (uri == null) uri = XMLConstants.NULL_NS_URI;
		
		for (Node self : this) {
			if (self == null) continue;
			
			switch (self.getNodeType()) {
			case Node.ELEMENT_NODE:
			case Node.ATTRIBUTE_NODE:
				String luri = uri;
				String lname = name;
				String lprefix = self.lookupPrefix(uri);
				if (lprefix == null && prefix != null && !prefix.isEmpty()) {
					lname = prefix + ":" + localName;
				}
				getOwner().doc.renameNode(self, luri, lname);
				break;
			}
		}
		
		return this;
	}
	
	public String attr(String name) {
		if (name == null) throw new IllegalArgumentException("name is null");
		if (isEmpty() || !(get(0) instanceof Element)) return null;
		
		String uri = null;
		String localName = null;
		
		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			uri = getOwner().xmlContext.getNamespaceURI(name.substring(0, index));
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return null;
		
		return ((Element)get(0)).getAttributeNS(uri, localName);
	}
	
	public Nodes attr(String name, String value) {
		if (name == null) throw new IllegalArgumentException("name is null");
		if (value == null) value = "";
		if (isEmpty()) return this;
		
		String uri = null;
		String prefix = null;
		String localName;
		
		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			prefix = name.substring(0, index);
			uri = getOwner().xmlContext.getNamespaceURI(prefix);
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return null;
		if (uri == null) uri = XMLConstants.NULL_NS_URI;
		
		for (Node self : this) {
			if (!(self instanceof Element)) continue;

			Element elem = (Element)self;
			String euri = elem.getNamespaceURI();
			if (euri == null) euri = XMLConstants.NULL_NS_URI;
			
			String luri = uri;
			String lname = name;
			String lprefix = self.lookupPrefix(uri);
			if (lprefix == null && prefix != null && !prefix.isEmpty()) {
				lname = prefix + ":" + localName;
			} else if (uri.equals(euri)) {
				luri = null;
				lname = localName;
			}
			
			elem.setAttributeNS(luri, lname, value);
		}
		return this;
	}
	
	public Nodes attr(Map<String, String> attrs) {
		if (attrs == null) return this;
		
		for (Map.Entry<String, String> entry : attrs.entrySet()) {
			attr(entry.getKey(), entry.getValue());
		}
		
		return this;
	}
	
	public Nodes attr(String name, Mapper<String> func) {
		if (name == null) throw new IllegalArgumentException("name is null");
		if (func == null) return this;
		
		String uri = null;
		String prefix = null;
		String localName;
		
		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			prefix = name.substring(0, index);
			uri = getOwner().xmlContext.getNamespaceURI(prefix);
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return null;
		if (uri == null) uri = XMLConstants.NULL_NS_URI;
		
		int i = 0;
		for (Node self : this) {
			if (self == null) continue;
			if (!(self instanceof Element))  continue;
			
			Element elem = (Element)self;
			String oval = elem.getAttributeNS(uri, localName);
			
			String nval = func.map(i, oval);
			if (nval == null) {
				elem.removeAttributeNS(uri, localName);
			} else if (!nval.equals(oval)) {
				String euri = elem.getNamespaceURI();
				if (euri == null) euri = XMLConstants.NULL_NS_URI;
				
				String luri = uri;
				String lname = name;
				String lprefix = self.lookupPrefix(uri);
				if (lprefix == null && prefix != null && !prefix.isEmpty()) {
					lname = prefix + ":" + localName;
				} else if (uri.equals(euri)) {
					luri = null;
					lname = localName;
				}
				
				elem.setAttributeNS(luri, lname, nval);
			}
			
			i++;
		}
		
		return this;
	}
	
	public Nodes removeAttr(String name) {
		if (name == null) return this;
		if (isEmpty()) return this;
		
		String uri = null;
		String localName = null;
		
		int index = name.indexOf(':');
		if (index > 0 && index < name.length()-1) {
			localName = name.substring(index + 1);
			uri = getOwner().xmlContext.getNamespaceURI(name.substring(0, index));
			if (uri == null) localName = name;
		} else {
			localName = name;
		}
		if (localName.isEmpty()) return this;
		
		for (Node self : this) {
			if (!(self instanceof Element)) continue;
			((Element)self).removeAttributeNS(uri, localName);
		}
		return this;
	}
	
	public Nodes val(String value) {
		if (isEmpty() || get(0) == null) return null;
		
		for (Node self : this) {
			if (self == null) continue;
			
			switch (self.getNodeType()) {
			case Node.ATTRIBUTE_NODE:
			case Node.CDATA_SECTION_NODE:
			case Node.COMMENT_NODE:
			case Node.PROCESSING_INSTRUCTION_NODE:
			case Node.TEXT_NODE:
				self.setNodeValue(value);
				break;
			}
		}
		return this;
	}
	
	public String val() {
		if (isEmpty() || get(0) == null) return null;
		
		Node self = get(0);
		switch (self.getNodeType()) {
		case Node.ATTRIBUTE_NODE:
		case Node.CDATA_SECTION_NODE:
		case Node.COMMENT_NODE:
		case Node.PROCESSING_INSTRUCTION_NODE:
		case Node.TEXT_NODE:
			return self.getNodeValue();
		}
		return null;
	}
	
	public Nodes eq(int index) {
		Node self = get(index);
		if (self != null) {
			return new Nodes(this, self);
		} else {
			return new Nodes(getOwner(), this, 0);
		}
	}
	
	public boolean is(String filter) {
		if (filter == null || isEmpty()) return false;
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		for (Node self : this) {
			if (getOwner().evaluate(expr, self, boolean.class)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean is(Visitor<Nodes> func) {
		if (func == null || isEmpty()) return false;
		
		int i = 0;
		for (Node self : this) {
			if (Boolean.TRUE.equals(func.visit(i, getOwner().translate(self)))) {
				return true;
			}
			i++;
		}
		
		return false;
	}
	
	public boolean is(Nodes nodes) {
		if (isEmpty() || nodes.isEmpty()) return false;
		
		for (Node self : this) {
			if (nodes.contains(self)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean is(Node node) {
		if (node == null || isEmpty()) return false;
		for (Node self : this) {
			if (node.equals(self)) {
				return true;
			}
		}
		return false;
	}
	
	public int index(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) return -1;
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		for (int i = 0; i < size(); i++) {
			if (getOwner().evaluate(expr, get(i), boolean.class)) {
				return i;
			}
		}
		return -1;
	}
	
	public int index(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return -1;
		if (isEmpty()) return -1;
		
		return index(nodes.get(0));
	}
	
	public int index(Node node) {
		for (int i = 0; i < size(); i++) {
			if (get(i) == node) return i;
		}
		return -1;
	}
	
	public Nodes each(Visitor<Nodes> func) {
		if (func == null || isEmpty()) {
			return this;
		}
		
		int i = 0;
		for (Node self : this) {
			if (Boolean.FALSE.equals(func.visit(i, getOwner().translate(self)))) {
				return this;
			}
			i++;
		}
		return this;
	}
	
	public Nodes has(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("child::node()[" + escapeFilter(filter) + "]");
		Nodes nodes = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (getOwner().evaluate(expr, self, boolean.class)) {
				nodes.add(self);
			}
		}
		unique(nodes);
		return nodes;
	}
	
	@Override
	public void add(int index, Node node) {
		if (isExternalNode(node)) {
			node = getOwner().doc.importNode(node, true);
		}
		super.add(index, node);
	}
	
	@Override
	public boolean add(Node node) {
		if (isExternalNode(node)) {
			node = getOwner().doc.importNode(node, true);
		}
		return super.add(node);
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends Node> c) {
		boolean result = super.addAll(index, c);
		if (result) {
			for (int i = index; i < index + c.size(); i++) {
				Node node = super.get(i);
				if (isExternalNode(node)) {
					super.set(i, getOwner().doc.importNode(node, true));
				}
			}
		}
		return result;
	}
	
	@Override
	public boolean addAll(Collection<? extends Node> c) {
		boolean result = super.addAll(c);
		if (result) {
			for (int i = 0; i < c.size(); i++) {
				Node node = super.get(i);
				if (isExternalNode(node)) {
					super.set(i, getOwner().doc.importNode(node, true));
				}
			}
		}
		return result;
	}
	
	@Override
	public Node set(int index, Node node) {
		if (isExternalNode(node)) {
			node = getOwner().doc.importNode(node, true);
		}
		return super.set(index, node);
	}
	
	public Nodes add(Nodes nodes) {
		if (nodes == null|| isEmpty()) {
			return new Nodes(this, this);
		}
		
		Nodes results = new Nodes(getOwner(), this, size() + nodes.size());
		results.addAll(this);
		results.addAll(nodes);
		unique(results);
		return results;
	}
	
	public Nodes add(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return new Nodes(this, this);
		}
		
		Object expr = getOwner().compileXPath(toFullXPath(xpath));
		
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		results.addAll(this);
		for (Node self : this) {
			NodeList list = getOwner().evaluate(expr, self, NodeList.class);
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) continue;
				results.add(node);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes addBack() {
		if (back == null || back.isEmpty() || back == this) {
			return new Nodes(this, this);
		}
		
		Nodes results = new Nodes(getOwner(), this, size() + back.size());
		results.addAll(this);
		results.addAll(back);
		unique(results);
		return results;
	}
	
	public Nodes addBack(String filter) {
		if (filter == null || back == null || back.isEmpty() || back == this) {
			return new Nodes(this, this);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		results.addAll(this);
		for (Node node : back) {
			if (getOwner().evaluate(expr, node, boolean.class)) {
				results.add(node);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes end() {
		if (back == null) {
			return new Nodes(getOwner(), null, 0);
		}
		return back;
	}
	
	public <T> T evaluate(String xpath, Class<T> cls) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return null;
		}
		
		Node self = get(0);
		if (self == null) return null;
		
		Object expr = getOwner().compileXPath(xpath);
		return getOwner().evaluate(expr, self, cls);
	}
	
	public Nodes select(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath(xpath);
		
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			NodeList list = getOwner().evaluate(expr, self, NodeList.class);
			for (int i = 0; i < list.getLength(); i++) {
				results.add(list.item(i));
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes find(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath(toFullXPath(xpath));
		
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			NodeList list = getOwner().evaluate(expr, self, NodeList.class);
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) continue;
				results.add(node);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes filter(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (getOwner().evaluate(expr, self, boolean.class)) {
				results.add(self);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes filter(Visitor<Nodes> func) {
		if (func == null || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Nodes results = new Nodes(getOwner(), this, size());
		int i = 0;
		for (Node self : this) {
			if (Boolean.TRUE.equals(func.visit(i, getOwner().translate(self)))) {
				results.add(self);
			}
			i++;
		}
		unique(results);
		return results;
	}
	
	public Nodes not(String filter) {
		if (filter == null || filter.isEmpty()) {
			return new Nodes(this, this);
		} else if (isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (!getOwner().evaluate(expr, self, boolean.class)) {
				results.add(self);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes parent() {
		return parentsInternal(SelectMode.FIRST);
	}
	
	public Nodes parent(String filter) {
		return parentsInternal(filter, SelectMode.FIRST);
	}
	
	public Nodes parentsUntil(String filter) {
		return parentsInternal(filter, SelectMode.UNTIL);
	}
	
	public Nodes parents() {
		return parentsInternal(SelectMode.ALL);
	}
	
	public Nodes parents(String filter) {
		return parentsInternal(filter, SelectMode.ALL);
	}
	
	Nodes parentsInternal(SelectMode mode) {
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node parent = self;
			while ((parent = parent.getParentNode()) != null) {
				if (parent.getNodeType() != Node.ELEMENT_NODE) break;
				results.add(parent);
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
			Collections.reverse(results);
		}
		return results;
	}
	
	Nodes parentsInternal(String filter, SelectMode mode) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node parent = self;
			while ((parent = parent.getParentNode()) != null) {
				if (parent.getNodeType() != Node.ELEMENT_NODE) break;
				
				if (getOwner().evaluate(expr, parent, boolean.class)) {
					results.add(parent);
					if (mode == SelectMode.UNTIL) break;
				} else if (mode == SelectMode.UNTIL) {
					results.add(parent);
				}
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
			Collections.reverse(results);
		}
		return results;
	}
	
	public Nodes closest(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node current = self;
			do {
				if (current.getNodeType() != Node.ELEMENT_NODE) break;
				
				if (getOwner().evaluate(expr, current, boolean.class)) {
					results.add(current);
					break;
				}
			} while ((current = current.getParentNode()) != null);
		}
		unique(results);
		return results;
	}
	
	public Nodes children() {
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				
				results.add(child);
			}
		}
		return results;
	}
	
	public Nodes children(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (getOwner().evaluate(expr, child, boolean.class)) {
					results.add(child);
				}
			}
		}
		return results;
	}
	
	public Nodes contents() {
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				
				results.add(child);
			}
		}
		return results;
	}
	
	public Nodes contents(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child == null) continue;
				
				if (getOwner().evaluate(expr, child, boolean.class)) {
					results.add(child);
				}
			}
		}
		return results;
	}
	
	public Nodes first() {
		return eq(0);
	}
	
	public Nodes last() {
		return eq(-1);
	}
	
	public Nodes prev() {
		return prevInternal(SelectMode.FIRST);
	}
	
	public Nodes prev(String filter) {
		return prevInternal(filter, SelectMode.FIRST);
	}
	
	public Nodes prevUntil(String filter) {
		return prevInternal(filter, SelectMode.UNTIL);
	}
	
	public Nodes prevAll() {
		return prevInternal(SelectMode.ALL);
	}
	
	public Nodes prevAll(String filter) {
		return prevInternal(filter, SelectMode.ALL);
	}
	
	Nodes prevInternal(SelectMode mode) {
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				results.add(prev);
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
			Collections.reverse(results);
		}
		return results;
	}
	
	Nodes prevInternal(String filter, SelectMode mode) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (getOwner().evaluate(expr, prev, boolean.class)) {
					results.add(prev);
					if (mode == SelectMode.UNTIL) break;
				} else if (mode == SelectMode.UNTIL) {
					results.add(prev);
				}
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
			Collections.reverse(results);
		}
		return results;
	}
	
	public Nodes next() {
		return nextInternal(SelectMode.FIRST);
	}
	
	public Nodes next(String filter) {
		return nextInternal(filter, SelectMode.FIRST);
	}
	
	public Nodes nextUntil(String filter) {
		return nextInternal(filter, SelectMode.UNTIL);
	}
	
	public Nodes nextAll() {
		return nextInternal(SelectMode.ALL);
	}
	
	public Nodes nextAll(String filter) {
		return nextInternal(filter, SelectMode.ALL);
	}
	
	Nodes nextInternal(SelectMode mode) {
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;
				
				results.add(next);
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
		}
		return results;
	}
	
	Nodes nextInternal(String filter, SelectMode mode) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (getOwner().evaluate(expr, next, boolean.class)) {
					results.add(next);
					if (mode == SelectMode.UNTIL) break;
				} else if (mode == SelectMode.UNTIL) {
					results.add(next);
				}
				if (mode == SelectMode.FIRST) break;
			}
		}
		if (mode != SelectMode.FIRST && results.size() > 1) {
			unique(results);
		}
		return results;
	}
	
	public Nodes siblings() {
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				
				results.add(prev);
			}
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;
				
				results.add(next);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes siblings(String filter) {
		if (filter == null || filter.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath("self::node()[" + escapeFilter(filter) + "]");
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (getOwner().evaluate(expr, prev, boolean.class)) {
					results.add(prev);
				}
			}
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;

				if (getOwner().evaluate(expr, next, boolean.class)) {
					results.add(next);
				}
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes slice(int start) {
		return slice(start, size());
	}
	
	public Nodes slice(int start, int end) {
		if (start < 0) start = size() + start;
		if (end < 0) end = size() + end;
		
		if (start < 0 || start >= size() || end <= 0 || end > size()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Nodes results = new Nodes(getOwner(), this, end-start);
		int pos = 0;
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			if (pos >= start && pos < end) results.add(self);
			pos++;
		}
		unique(results);
		return results;
	}
	
	public Nodes prepend(String xml) {
		if (xml == null || xml.isEmpty()) return this;
		
		return prepend(getOwner().parse(xml));
	}
	
	public Nodes prepend(Nodes nodes) {
		if (nodes == null) return this;
		
		boolean first = true;
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE
					&& self.getNodeType() != Node.DOCUMENT_NODE) continue;
			
			Node ref = self.getFirstChild();
			for (Node node : nodes) {
				if (node == null) continue;
				
				if (!first) node = node.cloneNode(true);
				if (ref != null) {
					self.insertBefore(node, ref);
				} else {
					self.appendChild(node);
				}
			}
			first = false;
		}
		return this;
	}
	
	public Nodes prependTo(String xpath) {
		return prependTo(getOwner().find(xpath));
	}
	
	public Nodes prependTo(Nodes nodes) {
		if (nodes == null) return new Nodes(getOwner(), this, 0);
		
		Nodes results = new Nodes(getOwner(), this, nodes.size());
		for (Node node : nodes) {
			if (node == null) continue;
			if (node.getNodeType() != Node.ELEMENT_NODE
					&& node.getNodeType() != Node.DOCUMENT_NODE) continue;
			
			Node ref = node.getFirstChild();
			for (Node self : this) {
				if (self == null) continue;
				self = self.cloneNode(true);
				results.add(self);
				if (ref != null) {
					node.insertBefore(self, ref);
				} else {
					node.appendChild(self);
				}
			}
		}
		return results;
	}
	
	public Nodes append(String xml) {
		if (xml == null || xml.isEmpty()) return this;
		
		return append(getOwner().parse(xml));
	}
	
	public Nodes append(Nodes nodes) {
		if (nodes.isEmpty()) return this;
		
		boolean first = true;
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE
					&& self.getNodeType() != Node.DOCUMENT_NODE) continue;
			
			for (Node node : nodes) {
				if (node == null) continue;
				if (!first) node = node.cloneNode(true);
				self.appendChild(node);
			}
			first = false;
		}
		return this;
	}
	
	public Nodes appendTo(String xpath) {
		return appendTo(getOwner().find(xpath));
	}
	
	public Nodes appendTo(Nodes nodes) {
		if (nodes == null) return new Nodes(getOwner(), this, 0);
		
		Nodes result = new Nodes(getOwner(), this, nodes.size());
		for (Node node : nodes) {
			if (node == null) continue;
			if (node.getNodeType() != Node.ELEMENT_NODE
					&& node.getNodeType() != Node.DOCUMENT_NODE) continue;

			for (Node self : this) {
				if (self == null) continue;
				self = self.cloneNode(true);
				result.add(self);
				node.appendChild(self);
			}
		}
		return result;
	}
	
	public Nodes before(String xml) {
		return before(getOwner().parse(xml));
	}
	
	public Nodes before(Nodes nodes) {
		if (nodes == null) return this;
		
		boolean first = true;
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			for (Node node : nodes) {
				if (node == null) continue;
				
				if (!first) node = node.cloneNode(true);
				parent.insertBefore(node, self);
			}
			first = false;
		}
		return this;
	}
	
	public Nodes insertBefore(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return this;
		}
		
		getOwner().find(xpath).before(this);
		return this;
	}
	
	public Nodes insertBefore(Nodes nodes) {
		if (nodes == null || nodes.isEmpty() || isEmpty()) {
			return this;
		}
		
		nodes.before(this);
		return this;
	}
	
	public Nodes after(String xml) {
		return after(getOwner().parse(xml));
	}
	
	public Nodes after(Nodes nodes) {
		if (nodes == null) return this;
		
		boolean first = true;
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;

			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node next = self.getNextSibling();
			for (Node node : nodes) {
				if (node == null) continue;
				
				if (!first) node = node.cloneNode(true);
				if (next != null) {
					parent.insertBefore(node, next);
				} else {
					parent.appendChild(node);
				}
			}
			first = false;
		}
		return this;
	}
	
	public Nodes insertAfter(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return this;
		}
		
		getOwner().find(xpath).after(this);
		return this;
	}
	
	public Nodes insertAfter(Nodes nodes) {
		if (nodes == null || nodes.isEmpty() || isEmpty()) {
			return this;
		}
		
		nodes.after(this);
		return this;
	}
	
	public Nodes wrap(String xml) {
		return wrap(getOwner().parse(xml));
	}
	
	public Nodes wrap(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return this;
		if (nodes.get(0) == null || nodes.get(0).getNodeType() != Node.ELEMENT_NODE) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node root = nodes.get(0).cloneNode(true);
			Node leaf = getFirstLeaf(root);
			leaf.appendChild(parent.replaceChild(root, self));
		}
		return this;
	}
	
	public Nodes wrapInner(String xml) {
		return wrapInner(getOwner().parse(xml));
	}
	
	public Nodes wrapInner(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return this;
		if (nodes.get(0) == null || nodes.get(0).getNodeType() != Node.ELEMENT_NODE) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node root = nodes.get(0).cloneNode(true);
			Node leaf = getFirstLeaf(root);
			while (self.hasChildNodes()) {
				leaf.appendChild(self.getFirstChild());
			}
			self.appendChild(root);
		}
		return this;
	}
	
	public Nodes wrapAll(String xml) {
		return wrapAll(getOwner().parse(xml));
	}
	
	public Nodes wrapAll(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return this;
		if (nodes.get(0) == null || nodes.get(0).getNodeType() != Node.ELEMENT_NODE) return this;
		if (isEmpty() || get(0) == null) return this;
		
		Node root = nodes.get(0);
		if (root.getParentNode() != null) {
			root = root.cloneNode(true);
		}
		Node leaf = getFirstLeaf(root);
		
		Node parent = get(0).getParentNode();
		if (parent == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;	
			
			if (root.getParentNode() == null) {
				leaf.appendChild(parent.replaceChild(root, self));
			} else {
				leaf.appendChild(self);
			}
		}
		return this;
	}
	
	public Nodes unwrap() {
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;

			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node next = self.getNextSibling();
			parent.removeChild(self);
			while (self.hasChildNodes()) {
				Node child = self.getFirstChild();
				if (next != null) {
					parent.insertBefore(child, next);
				} else {
					parent.appendChild(child);
				}
			}
		}
		return this;
	}
	
	public Nodes replaceWith(String xml) {
		return replaceWith(getOwner().parse(xml));
	}
	
	public Nodes replaceWith(Nodes nodes) {
		if (nodes == null) return this;
		
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			Node next = self.getNextSibling();
			parent.removeChild(self);
			
			for (Node node : nodes) {
				if (node.getParentNode() != null) {
					node = node.cloneNode(true);
				}
				if (next != null) {
					parent.insertBefore(node, next);
				} else {
					parent.appendChild(node);
				}
			}
		}
		return this;
	}
	
	public Nodes replaceAll(String xpath) {
		return replaceAll(getOwner().find(xpath));
	}
	
	public Nodes replaceAll(Nodes nodes) {
		if (nodes == null) return new Nodes(getOwner(), this, 0);
		
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node node : nodes) {
			if (node == null) continue;
			if (node.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = node.getParentNode();
			if (parent == null) continue;
			
			Node next = node.getNextSibling();
			parent.removeChild(node);
			for (Node self : this) {
				if (self.getParentNode() != null) {
					self = self.cloneNode(true);
				}
				if (next != null) {
					parent.insertBefore(self, next);
				} else {
					parent.appendChild(self);
				}
				results.add(self);
			}
		}
		unique(results);
		return results;
	}
	
	public Nodes empty() {
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			while (self.hasChildNodes()) {
				self.removeChild(self.getLastChild());
			}
		}
		return this;
	}

	public Nodes remove() {
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			Node parent = self.getParentNode();
			if (parent == null) continue;
			
			parent.removeChild(self);
		}
		return this;
	}
	
	public Nodes remove(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return this;
		}
		
		Object expr = getOwner().compileXPath(toFullXPath(xpath));
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE) continue;
			
			NodeList nodes = getOwner().evaluate(expr, self, NodeList.class);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				Node parent = node.getParentNode();
				if (parent == null) continue;
				
				parent.removeChild(node);
			}
		}
		return this;
	}
	
	public Nodes clone() {
		Nodes clone = new Nodes(getOwner(), back, size());
		for (Node self : this) {
			clone.add(self.cloneNode(true));
		}
		return clone;
	}
	
	public String xml() {
		if (isEmpty()) return "";
		
		NodeList nodes = get(0).getChildNodes();
		if (nodes.getLength() == 0) return "";
		
		XMLWriter serializer = new XMLWriter();
		serializer.setShowXMLDeclaration(false);
		StringWriter writer = new StringWriter();
		try {
			for (int i = 0; i < nodes.getLength(); i++) {
				serializer.writeTo(writer, nodes.item(i));
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return writer.toString();
	}
	
	public Nodes xml(String xml) {
		empty().append(xml);
		return this;
	}
	
	public String text() {
		if (isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder();
		for (Node self : this) {
			if (self instanceof Document) {
				self = ((Document)self).getDocumentElement();
			}
			
			if (self instanceof Element) {
				sb.append(self.getTextContent());
			}
		}
		return sb.toString();
	}
	
	public Nodes text(String text) {
		for (Node self : this) {
			self.setTextContent(text);
		}
		return this;
	}
	
	public Nodes normalize() {
		if (isEmpty()) return this;
		
		Object expr = getOwner().compileXPath("//namespace::*");
		for (Node self : this) {
			NodeList list = getOwner().evaluate(expr, self, NodeList.class);
			for (int i = 0; i < list.getLength(); i++) {
				Node ns = list.item(i);
				if (XMLConstants.XML_NS_URI.equals(ns.getNodeValue())) continue;
				if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(ns.getNodeValue())) continue;
				
				Node parent = ns.getParentNode();
				if (!(parent instanceof Element)) continue;
				
				expr = getOwner().compileXPath("//*[namespace-uri()='" + ns.getNodeValue() + "' or @*[namespace-uri()='" + ns.getNodeValue() + "']]");
				if (!getOwner().evaluate(expr, parent, boolean.class)) {
					if (ns.getNodeName() != null && !ns.getNodeName().isEmpty()) {
						((Element)parent).removeAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, ns.getNodeName());
					} else {
						((Element)parent).removeAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE);
					}
				}
			}
		}
		return this;
	}
	
	public int getLength() {
		return size();
	}
	
	@Override
	public String toString() {
		if (isEmpty()) return "";
		
		XMLWriter serializer = new XMLWriter();
		serializer.setShowXMLDeclaration(false);
		StringWriter writer = new StringWriter();
		try {
			for (Node self : this) {
				serializer.writeTo(writer, self);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return writer.toString();
	}
	
	boolean isExternalNode(Node node) {
		if (node == null) return false;
		if (node instanceof Document) return false;
		if (node.getOwnerDocument() == getOwner().doc) return false;
		
		return true;
	}
	
	static Node getFirstLeaf(Node node) {
		if (!node.hasChildNodes()) return node;
		
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child == null) continue;
			
			if (child.hasChildNodes()) {
				return getFirstLeaf(child);
			} else {
				return child;
			}
		}
		return null;
	}
	
	static void unique(final Nodes nodes) {
		if (nodes.size() < 2) return;
		
		Collections.sort(nodes, new Comparator<Node>() {
			@Override
			public int compare(Node a, Node b) {
				if (a == b) return 0;
				if (a == null) return -1;
				if (b == null) return 1;
				
				short compare = a.compareDocumentPosition(b);
				if (compare != 0) {
					if ((compare & Node.DOCUMENT_POSITION_DISCONNECTED) != 0) {
						if (a instanceof Document || contains(nodes.getOwner().doc, a)) {
							return -1;
						}
						if (b instanceof Document || contains(nodes.getOwner().doc, b)) {
							return 1;
						}
						return 0;
					}
					return (compare & Node.DOCUMENT_POSITION_FOLLOWING) != 0 ? -1 : 1;
				}
				return 0;
			}
		});
		
		int dis = 0;
		for (int i = 0; i < nodes.size(); i++) {
			Node current = nodes.get(i);
			if (i - dis - 1 >= 0 && current == nodes.get(i - dis - 1)) {
				dis++;
			} else if (dis > 0) {
				nodes.set(i - dis, current);
			}
		}
		for (int i = 0; i < dis; i++) {
			nodes.remove(nodes.size() - 1);
		}
	}
	
	static boolean contains(Node a, Node b) {
		Node bup = (b != null) ? b.getParentNode() : null;
		return (a == bup || (bup != null && bup instanceof Element 
				&& (a.compareDocumentPosition(bup) & Node.DOCUMENT_POSITION_CONTAINED_BY) != 0));
	}
	
	static String toFullXPath(String pattern) {
		return pattern;
	}
	
	static String escapeFilter(String filter) {
		StringBuilder sb = new StringBuilder(filter.length());
		int state = 0; // 0 ' 1 " 2
		int nest = 0;
		for (int i = 0; i < filter.length(); i++) {
			char c = filter.charAt(i);
			switch (c) {
			case '\'':
				if (state == 0) {
					state = 1;
				} else if (state == 1) {
					state = 0;
				}
				sb.append(c);
				break;
			case '"':
				if (state == 0) {
					state = 2;
				} else if (state == 2) {
					state = 0;
				}
				sb.append(c);
				break;
			case '[':
				if (state == 0) {
					nest++;
				}
				sb.append(c);
				break;
			case ']':
				if (state == 0) {
					nest--;
					for (int j = 0; j < -nest; j++) {
						sb.append('[');
						nest++;
					}
				}
				sb.append(c);
				break;
			default:
				sb.append(c);
			}
		}
		if (state == 1) {
			sb.append('\'');
		} else if (state == 2) {
			sb.append('"');
		}
		for (int i = 0; i < nest; i++) {
			sb.append(']');
		}
		return sb.toString();
	}
}
