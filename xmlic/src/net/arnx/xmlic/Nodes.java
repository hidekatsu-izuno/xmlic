package net.arnx.xmlic;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;

import net.arnx.xmlic.internal.util.NodeMatcher;
import net.arnx.xmlic.internal.util.NodeMatcher.MatchType;
import net.arnx.xmlic.internal.util.StatusImpl;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * A list of org.w3c.dom.Node, have traversal and manipulation API (like jQuery).
 */
public class Nodes extends ArrayList<Node> {
	private static final long serialVersionUID = 1L;
	private static final Pattern STYLE_PATTERN = Pattern.compile("\\G\\s*([^:]+)\\s*:\\s*([^;]+)\\s*(?:;+|$)");
		
	static enum SelectMode {
		FIRST,
		UNTIL,
		ALL
	}
	
	final XML owner;
	final Nodes back;
	
	/**
	 * Constructs empty Nodes instance.
	 * 
	 * @param owner Owner Document
	 */
	public Nodes(XML owner) {
		this(owner, null, 4);
	}
	
	/**
	 * Constructs a Nodes instance that has a specified DOM node.
	 * 
	 * @param owner Owner Document
	 * @param node DOM node
	 */
	public Nodes(XML owner, Node node) {
		this(owner, null, 1);
		if (node != null) {
			add(node);
		} else {
			throw new NullPointerException("node is null.");
		}
	}
	
	/**
	 * Constructs a Nodes instance that has specified DOM nodes.
	 * 
	 * @param owner Owner Document
	 * @param nodes DOM nodes
	 */
	public Nodes(XML owner, Node... nodes) {
		this(owner, null, nodes.length);
		for (int i = 0; i < nodes.length; i++) {
			add(nodes[i]);
		}
	}
	
	/**
	 * Constructs a Nodes instance that has nodes of DOM node collection.
	 * 
	 * @param owner Owner Document
	 * @param list DOM node collection
	 */
	public Nodes(XML owner, Collection<Node> list) {
		this(owner, null, list.size());
		addAll(list);
	}
	
	/**
	 * Constructs a Nodes instance that has nodes of DOM NodeList.
	 * 
	 * @param owner Owner Document
	 * @param list DOM NodeList
	 */
	public Nodes(XML owner, NodeList list) {
		this(owner, null, list.getLength());
		for (int i = 0; i < list.getLength(); i++) {
			add(list.item(i));
		}
	}
	
	Nodes(XML owner, Nodes back, int size) {
		super(size);
		if (owner == null) throw new NullPointerException("owner must not be null.");
		this.owner = owner;
		this.back = back;
	}
	
	/**
	 * Gets Owner Document.
	 * 
	 * @return Owner Document
	 */
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
	
	/**
	 * Gets a namespace URI of the first element.
	 * 
	 * @return a namespace URI of the first element
	 */
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
	
	/**
	 * Sets namespace URI of the first element.
	 * 
	 * @param uri namespace URI
	 * @return a reference to this object
	 */
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
	
	/**
	 * Removes all namespace of the current elements.
	 * 
	 * @return a reference to this object
	 */
	public Nodes removeNamespace() {
		return namespace(null);
	}
	
	/**
	 * Removes the specified namespace of the current elements.
	 * 
	 * @param uri namespace URI
	 * @return a reference to this object
	 */
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
	
	/**
	 * Gets a prefix of the first element.
	 * 
	 * @return a prefix of the first element
	 */
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
	
	/**
	 * Sets a prefix of the current elements.
	 * 
	 * @param prefix a prefix
	 * @return a reference to this object
	 */
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
	
	/**
	 * Gets a local name of the first element. 
	 * 
	 * @return a local name of the first element
	 */
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
	
	/**
	 * Sets a local name of the current elements.
	 * 
	 * @param localName a local name
	 * @return a reference to this object
	 */
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
	
	/**
	 * Gets a name of the first element. 
	 * 
	 * @return a name of the first element
	 */
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
	
	/**
	 * Sets a name of the current elements. 
	 * 
	 * @param name a attribute name
	 * @return a reference to this object
	 */
	public Nodes name(String name) {
		if (name == null) throw new NullPointerException("name must not be null");
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
	
	/**
	 * Sets a attribute value of the current elements. 
	 * 
	 * @param name a attribute name
	 * @return a reference to this object
	 */
	public String attr(String name) {
		if (name == null) throw new NullPointerException("name must not be null.");
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
	
	/**
	 * Gets a attribute value of the first element. 
	 * 
	 * @param name a name
	 * @return a attribute value of the first element
	 */
	public Nodes attr(String name, String value) {
		if (name == null) throw new NullPointerException("name must not be null.");
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
	
	/**
	 * Sets attributes of the current elements.
	 * 
	 * @param attrs attributes pairs
	 * @return a reference to this object
	 */
	public Nodes attr(Map<String, String> attrs) {
		if (attrs == null) return this;
		
		for (Map.Entry<String, String> entry : attrs.entrySet()) {
			attr(entry.getKey(), entry.getValue());
		}
		
		return this;
	}
	
	/**
	 * Filters a each attribute value of the current elements.
	 * 
	 * @param name a attribute name 
	 * @param func filter function
	 * @return a reference to this object
	 */
	public Nodes attr(String name, Filter<String> func) {
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
		
		StatusImpl state = new StatusImpl();
		try {
			for (Node self : this) {
				state.next(size()-1);
				if (self == null || !(self instanceof Element)) {
					continue;
				}
				
				Element elem = (Element)self;
				String oval = elem.getAttributeNS(uri, localName);
				String nval = func.filter(oval, state);
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
			}
		} catch (RuntimeException e) {
			if (!StatusImpl.isCancelException(e)) {
				throw e;
			}
		}
		
		return this;
	}
	
	/**
	 * Removes attribute of the specified name.
	 * 
	 * @param name a attribute name
	 * @return a reference to this object
	 */
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
	
	/**
	 * Set a node value of the every node.
	 * Note that this method differs from jQuery <code>val</code> method.
	 * 
	 * @see org.w3c.dom.Node#setNodeValue(String)
	 * @param value a new node value.
	 * @return a reference to this object. 
	 */
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
	
	/**
	 * Get a node value of the first node.
	 * Note that this method differs from jQuery <code>val</code> method.
	 * 
	 * @see org.w3c.dom.Node#getNodeValue()
	 * @return a node value of the first node.
	 */
	public String val() {
		if (isEmpty() || get(0) == null) return null;
		
		return get(0).getNodeValue();
	}
	
	/**
	 * Get a Nodes instance for a node at the specified index.
	 * -N is the relative index for last.
	 * 
	 * @param index a index number
	 * @return a Nodes instance for a node at the specified index
	 */
	public Nodes eq(int index) {
		Node self = get(index);
		if (self != null) {
			Nodes nodes = new Nodes(getOwner(), this, 1);
			nodes.add(self);
			return nodes;
		} else {
			return new Nodes(getOwner(), this, 0);
		}
	}
	
	/**
	 * Checks current nodes matched against a specified pattern.
	 * 
	 * @param pattern xpath pattern
	 * @return true if at least one of these nodes matches
	 */
	public boolean is(String pattern) {
		if (pattern == null || isEmpty()) return false;
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		for (Node self : this) {
			if (m.match(self)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks current nodes matched against a specified filter function.
	 * 
	 * @param func a filter function
	 * @return true if at least one of these nodes matches
	 */
	public boolean is(Judge<Nodes> func) {
		if (func == null || isEmpty()) return false;
		
		StatusImpl state = new StatusImpl();
		try {
			for (Node self : this) {
				state.next(size()-1);
				if (func.accept(new Nodes(getOwner(), self), state)) {
					return true;
				}
			}
		} catch (RuntimeException e) {
			if (!StatusImpl.isCancelException(e)) {
				throw e;
			}
		}
		return false;
	}
	
	/**
	 * Checks current nodes matched against a specified set of nodes.
	 * 
	 * @param nodes a Nodes object
	 * @return true if at least one of these nodes matches
	 */
	public boolean is(Nodes nodes) {
		if (isEmpty() || nodes.isEmpty()) return false;
		
		for (Node self : this) {
			if (nodes.contains(self)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Checks current nodes matched against a specified node.
	 * 
	 * @param node a node
	 * @return true if at least one of these nodes matches
	 */
	public boolean is(Node node) {
		if (node == null || isEmpty()) return false;
		for (Node self : this) {
			if (node.equals(self)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets a index number of the first element matched a specified pattern.
	 * 
	 * @param pattern a XPath pattern
	 * @return the 0-based position if a matched node exists. else -1
	 */
	public int index(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) return -1;
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		for (int i = 0; i < size(); i++) {
			if (m.match(get(i))) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Gets a index number of the first element matched the first node of a specified Nodes.
	 * 
	 * @param nodes a Nodes object
	 * @return the 0-based position if a matched node exists. else -1
	 */
	public int index(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) return -1;
		if (isEmpty()) return -1;
		
		return index(nodes.get(0));
	}
	
	/**
	 * Gets a index number of the first element matched a specified node.
	 * 
	 * @param nodes a node
	 * @return a index number if a matched node exists. else -1
	 */
	public int index(Node node) {
		for (int i = 0; i < size(); i++) {
			if (get(i) == node) return i;
		}
		return -1;
	}
	
	/**
	 * Gets a mapped value's list from current nodes.
	 * 
	 * @param func a mapper function
	 * @return a mapped value's list
	 */
	public List<String> map(Mapper<Nodes, String> func) {
		if (func == null || isEmpty()) {
			return new ArrayList<String>(0);
		}
		
		StatusImpl status = new StatusImpl();
		List<String> result = new ArrayList<String>();
		try {
			for (Node self : this) {
				status.next(size()-1);
				String value = func.map(new Nodes(getOwner(), self), status);
				if (value != null) {
					result.add(value);
				}
			}
		} catch (RuntimeException e) {
			if (!StatusImpl.isCancelException(e)) {
				throw e;
			}
		} 
		return result;
	}
	
	/**
	 * Iterates a each node of current nodes.
	 * 
	 * @param func a visitor function
	 * @return a reference to this object
	 */
	public Nodes each(Visitor<Nodes> func) {
		return each(func, false);
	}
	
	/**
	 * Iterates a each node of current nodes.
	 * 
	 * @param func a visitor function
	 * @param reverse true if you wish to iterate reverse.
	 * @return a reference to this object
	 */
	public Nodes each(Visitor<Nodes> func, boolean reverse) {
		if (func == null || isEmpty()) {
			return this;
		}
		
		StatusImpl status = new StatusImpl();
		try {
			ListIterator<Node> i = listIterator(reverse ? size() : 0);
			while (reverse ? i.hasPrevious() : i.hasNext()) {
				status.next(size()-1);
				func.visit(new Nodes(getOwner(), reverse ? i.previous() : i.next()), status);
			}
		} catch (RuntimeException e) {
			if (!StatusImpl.isCancelException(e)) {
				throw e;
			}
		}
		return this;
	}
	
	/**
	 * Reduces the set of elements to those that have a descendant that matched a specified pattern.
	 * 
	 * @param pattern a XPath pattern
	 * @return the set of elements  to those that have a descendant that matched
	 */
	public Nodes has(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes nodes = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			NodeList children = self.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				if (m.match(children.item(j))) {
					nodes.add(self);
					break;
				}
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
	
	/**
	 * Adds elements that matched pattern.
	 * 
	 * @param pattern a pattern
	 * @return a reference to this object
	 */
	public Nodes add(String pattern) {
		return add(getOwner().find(pattern));
	}
	
	/**
	 * Adds a set of specified nodes.
	 * 
	 * @param nodes a set of nodes
	 * @return a reference to this object
	 */
	public Nodes add(Nodes nodes) {
		if (nodes == null || nodes.isEmpty()) {
			Nodes results = new Nodes(getOwner(), this, size());
			results.addAll(this);
			return results;
		}
		
		Nodes results = new Nodes(getOwner(), this, size() + nodes.size());
		results.addAll(this);
		results.addAll(nodes);
		unique(results);
		return results;
	}
	
	/**
	 * Adds the previous set of nodes.
	 * 
	 * @return a reference to this object
	 */
	public Nodes addBack() {
		if (back == null || back.isEmpty() || back == this) {
			Nodes results = new Nodes(getOwner(), this, size());
			results.addAll(this);
			return results;
		}
		
		Nodes results = new Nodes(getOwner(), this, size() + back.size());
		results.addAll(this);
		results.addAll(back);
		unique(results);
		return results;
	}
	
	/**
	 * Adds the previous set of nodes that filtered by a specified pattern.
	 * 
	 * @return a reference to this object
	 */
	public Nodes addBack(String pattern) {
		if (pattern == null || back == null || back.isEmpty() || back == this) {
			Nodes results = new Nodes(getOwner(), this, size());
			results.addAll(this);
			return results;
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		results.addAll(this);
		for (Node node : back) {
			if (m.match(node)) {
				results.add(node);
			}
		}
		unique(results);
		return results;
	}
	
	/**
	 * Gets the previous set of nodes.
	 * 
	 * @return a reference to this object
	 */
	public Nodes end() {
		if (back == null) {
			return new Nodes(getOwner(), null, 0);
		}
		return back;
	}
	
	/**
	 * Evaluate a specified XPath expression at a first node of current nodes.
	 * And gets as a specified type.
	 * 
	 * @param xpath a XPath expression
	 * @param cls a result type
	 * @return a result value.
	 */
	public <T> T evaluate(String xpath, Class<T> cls) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return null;
		}
		
		Node self = get(0);
		if (self == null) return null;
		
		Object expr = getOwner().compileXPath(xpath, false);
		return getOwner().evaluate(expr, self, cls);
	}
	
	/**
	 * Find nodes by a specified XPath expression.
	 * 
	 * @param xpath a XPath expression
	 * @return a set of nodes
	 */
	public Nodes select(String xpath) {
		if (xpath == null || xpath.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath(xpath, false);
		
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
	
	/**
	 * Find elements by a specified XPath pattern.
	 * 
	 * @param pattern a XPath pattern
	 * @return a set of elements
	 */
	public Nodes find(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Object expr = getOwner().compileXPath(pattern, true);
		
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
	
	/**
	 * Filters a set of current nodes with a specified pattern.
	 * 
	 * @param pattern a pattern
	 * @return a filtered set of nodes
	 */
	public Nodes filter(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (m.match(self)) {
				results.add(self);
			}
		}
		unique(results);
		return results;
	}
	
	/**
	 * Filters a set of current nodes with a filter function.
	 * 
	 * @param func a filter function
	 * @return a filtered set of nodes
	 */
	public Nodes filter(Judge<Nodes> func) {
		if (func == null || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		Nodes results = new Nodes(getOwner(), this, size());
		StatusImpl state = new StatusImpl();
		try {
			for (Node self : this) {
				state.next(size()-1);
				if (func.accept(new Nodes(getOwner(), self), state)) {
					results.add(self);
				}
			}
		} catch (RuntimeException e) {
			if (!StatusImpl.isCancelException(e)) {
				throw e;
			}
		}
		unique(results);
		return results;
	}
	
	/**
	 * Get a set of nodes that not matched a specified pattern.
	 * 
	 * @param pattern a pattern
	 * @return a set of nodes that not matched a specified pattern
	 */
	public Nodes not(String pattern) {
		if (pattern == null || pattern.isEmpty()) {
			Nodes results = new Nodes(getOwner(), this, size());
			results.addAll(this);
			return results;
		} else if (isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (!m.match(self)) {
				results.add(self);
			}
		}
		unique(results);
		return results;
	}
	
	/**
	 * Traverses descendants of current nodes that matched a specified pattern.
	 * 
	 * @param pattern a pattern
	 * @param func a visitor function
	 * @return a reference of this object
	 */
	public Nodes traverse(String pattern, Visitor<Nodes> func) {
		return traverse(pattern, func, false);
	}
	
	/**
	 * Traverses descendants of current nodes that matched a specified pattern.
	 * 
	 * @param pattern a pattern
	 * @param func a visitor function
	 * @param reverse true if you wish to iterate reverse
	 * @return a reference of this object
	 */
	public Nodes traverse(String pattern, Visitor<Nodes> func, boolean reverse) {
		if (pattern == null || func == null) {
			return this;
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		if (m.getMatchType() == MatchType.NO_NODE) {
			return this;
		}
		
		int filter = toFilter(m.getMatchType());
		
		DocumentTraversal dt = (DocumentTraversal)getOwner().get();
		StatusImpl status = new StatusImpl();
		try {
			int i = 0;
			for (Node self : this) {
				NodeIterator ite = dt.createNodeIterator(self, filter, null, true);
				try {
					Node prev = null;
					Node node;
					while ((node = (reverse) ? ite.previousNode() : ite.nextNode()) != null) {
						if (m.getMatchType() == MatchType.ATTRIBUTE_NODE
								|| m.getMatchType() == MatchType.NAMESPACE_NODE
								|| m.getMatchType() == MatchType.ANY_NODE
								|| m.getMatchType() == MatchType.UNKNOWN_NODE) {
							
							NamedNodeMap attrs = node.getAttributes();
							for (int pos = 0; pos < attrs.getLength(); pos++) {
								Node attr = attrs.item(pos);
								if (prev != null) {
									status.next(-1);
									func.visit(new Nodes(getOwner(), prev), status);
									prev = null;
								}
								if (m.match(attr)) {
									prev = attr;
								}
							}
						}
						
						if (m.getMatchType() != MatchType.ATTRIBUTE_NODE
								&& m.getMatchType() != MatchType.NAMESPACE_NODE) {
							if (prev != null) {
								status.next(-1);
								func.visit(new Nodes(getOwner(), prev), status);
								prev = null;
							}
							if (m.match(node)) {
								prev = node;
							}
						}
					}
					if (prev != null) {
						status.next((i == size()-1) ? status.getIndex() + 1 : -1);
						func.visit(new Nodes(getOwner(), prev), status);
					}
				} finally {
					i++;
					ite.detach();
				}
			}
		} catch (RuntimeException e) {
			if (!StatusImpl.isCancelException(e)) {
				throw e;
			}
		}
		
		return this;
	}
	
	/**
	 * Gets the set of the parent element for current nodes.
	 * 
	 * @return the set of the parent element
	 */
	public Nodes parent() {
		return parentsInternal(SelectMode.FIRST);
	}
	
	/**
	 * Gets the set of the parent element for current nodes, and filters with a specified pattern.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the parent element
	 */
	public Nodes parent(String pattern) {
		return parentsInternal(pattern, SelectMode.FIRST);
	}
	
	/**
	 * Collects the set of the parent element for current nodes until matches a specified pattern.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the parent element
	 */
	public Nodes parentsUntil(String pattern) {
		return parentsInternal(pattern, SelectMode.UNTIL);
	}
	
	/**
	 * Gets the set of the ancestor elements for current nodes.
	 * 
	 * @return the set of the ancestor elements
	 */
	public Nodes parents() {
		return parentsInternal(SelectMode.ALL);
	}
	
	/**
	 * Gets the filtered set of the ancestor elements for current nodes.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the ancestor elements
	 */
	public Nodes parents(String pattern) {
		return parentsInternal(pattern, SelectMode.ALL);
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
	
	Nodes parentsInternal(String pattern, SelectMode mode) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node parent = self;
			while ((parent = parent.getParentNode()) != null) {
				if (parent.getNodeType() != Node.ELEMENT_NODE) break;
				
				if (m.match(parent)) {
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
	
	/**
	 * Collects the set of the ancestors or self elements for current nodes until matches a pattern.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the ancestor or self elements
	 */
	public Nodes closest(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node current = self;
			do {
				if (current.getNodeType() != Node.ELEMENT_NODE) break;
				
				if (m.match(current)) {
					results.add(current);
					break;
				}
			} while ((current = current.getParentNode()) != null);
		}
		unique(results);
		return results;
	}
	
	/**
	 * Gets the set of the child elements for current nodes.
	 * 
	 * @return the set of the child elements
	 */
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
	
	/**
	 * Gets the filtered set of the child elements for current nodes.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of child elements
	 */
	public Nodes children(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				if (child == null) continue;
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (m.match(child)) {
					results.add(child);
				}
			}
		}
		return results;
	}
	
	/**
	 * Gets the set of the child nodes for current nodes.
	 * 
	 * @return the set of child nodes
	 */
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
	
	/**
	 * Gets the filtered set of the child nodes for current nodes.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the child nodes
	 */
	public Nodes contents(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (!self.hasChildNodes()) continue;
			
			NodeList children = self.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				if (child == null) continue;
				
				if (m.match(child)) {
					results.add(child);
				}
			}
		}
		return results;
	}
	
	/**
	 * Gets the first node of current nodes.
	 * 
	 * @return the first node
	 */
	public Nodes first() {
		return eq(0);
	}
	
	/**
	 * Gets the last node of current nodes.
	 * 
	 * @return the last node
	 */
	public Nodes last() {
		return eq(-1);
	}
	
	/**
	 * Gets the set of the previous element for current nodes.
	 * 
	 * @return the set of the previous element
	 */
	public Nodes prev() {
		return prevInternal(SelectMode.FIRST);
	}
	
	/**
	 * Gets the filtered set of the previous element for current nodes.
	 * 
	 * @return the filtered set of the previous element
	 */
	public Nodes prev(String pattern) {
		return prevInternal(pattern, SelectMode.FIRST);
	}
	
	/**
	 * Collects the set of the previous element for current nodes until matches a specified pattern.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the previous element
	 */
	public Nodes prevUntil(String pattern) {
		return prevInternal(pattern, SelectMode.UNTIL);
	}
	
	/**
	 * Gets the set of the all previous elements for current nodes.
	 * 
	 * @return the set of the all previous elements
	 */
	public Nodes prevAll() {
		return prevInternal(SelectMode.ALL);
	}
	
	/**
	 * Gets the filtered set of the all previous elements for current nodes.
	 * 
	 * @return the filtered set of the all previous elements
	 */
	public Nodes prevAll(String pattern) {
		return prevInternal(pattern, SelectMode.ALL);
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
	
	Nodes prevInternal(String pattern, SelectMode mode) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (m.match(prev)) {
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
	
	/**
	 * Gets the set of the next element for current nodes.
	 * 
	 * @return the set of the next element
	 */
	public Nodes next() {
		return nextInternal(SelectMode.FIRST);
	}
	
	/**
	 * Gets the filtered set of the next element for current nodes.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the next element
	 */
	public Nodes next(String pattern) {
		return nextInternal(pattern, SelectMode.FIRST);
	}
	
	/**
	 * Collects the set of the next element for current nodes until matches a specified pattern.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the next element
	 */
	public Nodes nextUntil(String pattern) {
		return nextInternal(pattern, SelectMode.UNTIL);
	}
	
	/**
	 * Gets the set of the all next elements for current nodes.
	 * 
	 * @return the set of the all next elements
	 */
	public Nodes nextAll() {
		return nextInternal(SelectMode.ALL);
	}
	
	/**
	 * Gets the filtered set of the all next elements for current nodes.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the all next elements
	 */
	public Nodes nextAll(String pattern) {
		return nextInternal(pattern, SelectMode.ALL);
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
	
	Nodes nextInternal(String pattern, SelectMode mode) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size() * 2);
		for (Node self : this) {
			if (self == null) continue;
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (m.match(next)) {
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
	
	/**
	 * Gets the set of the all previous or next elements for current nodes.
	 * 
	 * @return the set of the all previous or next elements
	 */
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
	
	/**
	 * Gets the filtered set of the all previous or next elements for current nodes.
	 * 
	 * @param pattern a pattern
	 * @return the filtered set of the all previous or next elements
	 */
	public Nodes siblings(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return new Nodes(getOwner(), this, 0);
		}
		
		NodeMatcher m = getOwner().compileXPathPattern(pattern);
		Nodes results = new Nodes(getOwner(), this, size());
		for (Node self : this) {
			if (self == null) continue;
			
			Node prev = self;
			while ((prev = prev.getPreviousSibling()) != null) {
				if (prev.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if (m.match(prev)) {
					results.add(prev);
				}
			}
			
			Node next = self;
			while ((next = next.getNextSibling()) != null) {
				if (next.getNodeType() != Node.ELEMENT_NODE) continue;

				if (m.match(next)) {
					results.add(next);
				}
			}
		}
		unique(results);
		return results;
	}
	
	/**
	 * Reduces the set of nodes to a subset specified by a range of indices.
	 * 
	 * @param start the 0-based start position. If negative, it indicates an offset from the end
	 * @return the sliced set of nodes
	 */
	public Nodes slice(int start) {
		return slice(start, size());
	}
	
	/**
	 * Reduces the set of nodes to a subset specified by a range of indices.
	 * 
	 * @param start the 0-based start position. If negative, it indicates an offset from the end
	 * @param end the 0-based end position. If negative, it indicates an offset from the end
	 * @return the sliced set of nodes
	 */
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
	
	/**
	 * Inserts specified XML contents to the beginning of each element in the set of the elements.
	 * 
	 * @param xml XML contents
	 * @return a reference of this object
	 */
	public Nodes prepend(String xml) {
		if (xml == null || xml.isEmpty()) return this;
		
		return prepend(getOwner().parse(xml));
	}
	
	/**
	 * Inserts specified nodes to the beginning of each element in the set of the elements.
	 * 
	 * @param nodes inserting nodes
	 * @return a reference of this object
	 */
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
	
	/**
	 * Insert every element in the set of nodes to the beginning of the target.
	 * 
	 * @param pattern a pattern
	 * @return the inserted nodes
	 */
	public Nodes prependTo(String pattern) {
		return prependTo(getOwner().find(pattern));
	}
	
	/**
	 * Insert every element in the set of nodes to the beginning of the target.
	 * 
	 * @param nodes target nodes
	 * @return the inserted nodes
	 */
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
	
	/**
	 * Inserts specified XML contents to the end of each element in the set of the elements.
	 * 
	 * @param xml XML contents
	 * @return a reference of this object
	 */
	public Nodes append(String xml) {
		if (xml == null || xml.isEmpty()) return this;
		
		return append(getOwner().parse(xml));
	}
	
	/**
	 * Inserts specified nodes to the end of each element in the set of the elements.
	 * 
	 * @param nodes inserting nodes
	 * @return a reference of this object
	 */
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
	
	/**
	 * Insert every element in the set of nodes to the end of the target.
	 * 
	 * @param pattern a pattern
	 * @return the inserted nodes
	 */
	public Nodes appendTo(String pattern) {
		return appendTo(getOwner().find(pattern));
	}
	
	/**
	 * Insert every element in the set of nodes to the end of the target.
	 * 
	 * @param nodes target nodes
	 * @return the inserted nodes
	 */
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
	
	/**
	 * Inserts specified XML contents before each element in the set of the elements.
	 * 
	 * @param xml XML contents
	 * @return a reference of this object
	 */
	public Nodes before(String xml) {
		return before(getOwner().parse(xml));
	}
	
	/**
	 * Inserts specified nodes before each element in the set of the elements.
	 * 
	 * @param nodes inserting nodes
	 * @return a reference of this object
	 */
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
	
	/**
	 * Insert every element in the set of nodes before the target.
	 * 
	 * @param pattern a pattern
	 * @return the inserted nodes
	 */
	public Nodes insertBefore(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return this;
		}
		
		getOwner().find(pattern).before(this);
		return this;
	}
	
	/**
	 * Insert every element in the set of nodes before the target.
	 * 
	 * @param nodes target nodes
	 * @return the inserted nodes
	 */
	public Nodes insertBefore(Nodes nodes) {
		if (nodes == null || nodes.isEmpty() || isEmpty()) {
			return this;
		}
		
		nodes.before(this);
		return this;
	}
	
	/**
	 * Inserts specified XML contents after each element in the set of the elements.
	 * 
	 * @param xml XML contents
	 * @return a reference of this object
	 */
	public Nodes after(String xml) {
		return after(getOwner().parse(xml));
	}
	
	/**
	 * Inserts specified nodes after each element in the set of the elements.
	 * 
	 * @param nodes inserting nodes
	 * @return a reference of this object
	 */
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
	
	/**
	 * Inserts every element in the set of nodes after the target.
	 * 
	 * @param pattern a pattern
	 * @return the inserted nodes
	 */
	public Nodes insertAfter(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return this;
		}
		
		getOwner().find(pattern).after(this);
		return this;
	}
	
	/**
	 * Inserts every element in the set of nodes after the target.
	 * 
	 * @param nodes target nodes
	 * @return the inserted nodes
	 */
	public Nodes insertAfter(Nodes nodes) {
		if (nodes == null || nodes.isEmpty() || isEmpty()) {
			return this;
		}
		
		nodes.after(this);
		return this;
	}
	
	/**
	 * Wraps an XML structure around each element in the set of current elements.
	 * 
	 * @param xml wrapping XML contents
	 * @return a reference to this object
	 */
	public Nodes wrap(String xml) {
		return wrap(getOwner().parse(xml));
	}
	
	/**
	 * Wraps an XML structure around each element in the set of current elements.
	 * 
	 * @param nodes wrapping nodes
	 * @return a reference to this object
	 */
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
	
	/**
	 * Wraps an XML structure around the content of each element in the set of current elements.
	 * 
	 * @param xml wrapping XML contents
	 * @return a reference to this object
	 */
	public Nodes wrapInner(String xml) {
		return wrapInner(getOwner().parse(xml));
	}
	
	/**
	 * Wraps an XML structure around the content of each element in the set of current elements.
	 * 
	 * @param nodes wrapping nodes
	 * @return a reference to this object
	 */
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
	
	/**
	 * Wraps an XML structure around all elements in the set of current elements
	 * 
	 * @param xml wrapping XML contents
	 * @return a reference to this object.
	 */
	public Nodes wrapAll(String xml) {
		return wrapAll(getOwner().parse(xml));
	}
	
	/**
	 * Wraps an XML structure around all elements in the set of current elements
	 * 
	 * @param nodes wrapping nodes
	 * @return a reference to this object.
	 */
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
	
	/**
	 * Removes the parents of the set of current elements, leaving the elements in their place.
	 * 
	 * @return a reference to this object
	 */
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
	
	/**
	 * Replace each element in the set of current elements with the provided new content 
	 * and return the set of elements that was removed.
	 * 
	 * @param xml replacing XML contents 
	 * @return a reference to this object
	 */
	public Nodes replaceWith(String xml) {
		return replaceWith(getOwner().parse(xml));
	}
	
	/**
	 * Replace each element in the set of current elements with the provided new content 
	 * and return the set of elements that was removed.
	 * 
	 * @param nodes replacing nodes 
	 * @return a reference to this object
	 */	
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
	
	/**
	 * Replace each target element with the set of current elements.
	 * 
	 * @param pattern a pattern
	 * @return a reference to this object
	 */
	public Nodes replaceAll(String pattern) {
		return replaceAll(getOwner().find(pattern));
	}
	
	/**
	 * Replace each target element with the set of current elements.
	 * 
	 * @param nodes replacing nodes
	 * @return a reference to this object
	 */
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
	
	/**
	 * Removes all child nodes of the set of current nodes.
	 * 
	 * @return a reference to this object
	 */
	public Nodes empty() {
		for (Node self : this) {
			if (self == null) continue;
			if (self.getNodeType() != Node.ELEMENT_NODE 
					&& self.getNodeType() != Node.DOCUMENT_NODE) continue;
			
			while (self.hasChildNodes()) {
				self.removeChild(self.getLastChild());
			}
		}
		return this;
	}
	
	/**
	 * Removes the set of current nodes.
	 * 
	 * @return a reference to this object
	 */
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
	
	/**
	 * Removes the filtered set of current nodes.
	 * 
	 * @param pattern a pattern
	 * @return a reference to this object
	 */
	public Nodes remove(String pattern) {
		if (pattern == null || pattern.isEmpty() || isEmpty()) {
			return this;
		}
		
		Object expr = getOwner().compileXPath(pattern, true);
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
	
	@Override
	public Nodes clone() {
		Nodes clone = new Nodes(getOwner(), back, size());
		for (Node self : this) {
			clone.add(self.cloneNode(true));
		}
		return clone;
	}
	
	/**
	 * Get a XML text for the children of the first node.
	 * 
	 * @return a XML text for the children of the first node
	 */
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
	
	/**
	 * Sets the XML contents of each element in the set of current nodes.
	 * 
	 * @param xml a XML contents
	 * @return a reference of this object
	 */
	public Nodes xml(String xml) {
		empty().append(xml);
		return this;
	}
	
	/**
	 * Gets a concatenated text of current nodes.
	 * 
	 * @return a concatenated text of current nodes
	 */
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
	
	/**
	 * Sets the text contents of each element in the set of current nodes.
	 * 
	 * @param text a text contents
	 * @return a reference of this object
	 */
	public Nodes text(String text) {
		for (Node self : this) {
			self.setTextContent(text);
		}
		return this;
	}
	
	/**
	 * Gets the XPath full path.
	 * 
	 * @return the XPath full path
	 */
	public String xpath() {
		if (isEmpty() || get(0) == null) {
			return "";
		}
		
		List<Node> list = new ArrayList<Node>();
		Node current = get(0);
		while (current != null && current instanceof Element) {
			list.add(current);
			current = current.getParentNode();
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Node node = list.get(list.size()-i-1);
			sb.append("/").append(node.getNodeName());
		}
		return sb.toString();
	}
	
	/**
	 * Normalizes the set of current nodes.
	 * This method executes below two action:
	 * - executes {@link org.w3c.dom.Node#normalize()}. 
	 * - removes waste namespace declaration.
	 * 
	 * @return a reference to this object
	 */
	public Nodes normalize() {
		if (isEmpty()) return this;
		
		Object expr = getOwner().compileXPath("//namespace::*", false);
		for (Node self : this) {
			NodeList list = getOwner().evaluate(expr, self, NodeList.class);
			for (int i = 0; i < list.getLength(); i++) {
				Node ns = list.item(i);
				if (XMLConstants.XML_NS_URI.equals(ns.getNodeValue())) continue;
				if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(ns.getNodeValue())) continue;
				
				Node parent = ns.getParentNode();
				if (!(parent instanceof Element)) continue;
				
				expr = getOwner().compileXPath("//*[namespace-uri()='" + ns.getNodeValue() + "' or @*[namespace-uri()='" + ns.getNodeValue() + "']]", false);
				if (!getOwner().evaluate(expr, parent, boolean.class)) {
					if (ns.getNodeName() != null && !ns.getNodeName().isEmpty()) {
						((Element)parent).removeAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, ns.getNodeName());
					} else {
						((Element)parent).removeAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE);
					}
				}
			}
			self.normalize();
		}
		return this;
	}
	
	public boolean hasClass(String classes) {
		if (classes == null || classes.isEmpty()) return false;
		
		String target = attr("class");
		if (target == null || target.isEmpty()) return false;
		
		int start = 0;
		for (int i = 0; i < classes.length(); i++) {
			char c = classes.charAt(i);
			if (c == ' ' || i == classes.length()-1) {
				int end = (c == ' ') ? i : i+1;
				if (start < end) {
					boolean match = false;
					int pos = 0;
					for (int j = 0; j < target.length() + 2; j++) {
						char s = (j == 0 || j > target.length()) ? ' ' : target.charAt(j-1);
						char d = (pos == 0 || pos > end-start) ? ' ' : classes.charAt(start+pos-1);
						if (s == d) {
							pos++;
							if (pos == end-start+2) {
								match = true;
								break;
							}
						} else {
							pos = 0;
						}
					}
					if (!match) return false;
				}
				start = end + 1;
			}
		}
		return true;
	}
	
	public Nodes addClass(String classes) {
		return toggleClassInternal(classes, true);
	}
	
	public Nodes removeClass(String classes) {
		return toggleClassInternal(classes, false);
	}
	
	public Nodes toggleClass(String classes, boolean toggle) {
		return toggleClassInternal(classes, toggle);
	}
	
	private Nodes toggleClassInternal(String classes, Boolean toggle) {
		if (classes == null || classes.isEmpty()) return this;
		
		Set<String> set = new LinkedHashSet<String>();
		
		String target = attr("class");
		int start = 0;
		if (target != null) {
			for (int i = 0; i < target.length(); i++) {
				char c = target.charAt(i);
				if (c == ' ' || i == target.length()-1) {
					int end = (c == ' ') ? i : i+1;
					if (start < end) {
						set.add(target.substring(start, end));
					}
					start = end + 1;
				}
			}
		}
		
		start = 0;
		for (int i = 0; i < classes.length(); i++) {
			char c = classes.charAt(i);
			if (c == ' ' || i == classes.length()-1) {
				int end = (c == ' ') ? i : i+1;
				if (start < end) {
					String cls = classes.substring(start, end);
					if (toggle == null) {
						if (set.contains(cls)) {
							set.remove(cls);
						} else {
							set.add(cls);
						}
					} else if (toggle) {
						if (!set.contains(cls)) set.add(cls);
					} else {
						if (set.contains(cls)) set.remove(cls);
					}
				}
				start = end + 1;
			}
		}
		if (set.size() == 0) {
			removeAttr("class");
		} else if (set.size() == 1) {
			attr("class", set.iterator().next());
		} else {
			StringBuilder sb = new StringBuilder();
			for (String cls : set) {
				sb.append(' ').append(cls);
			}
			attr("class", sb.substring(1));
		}
		return this;
	}
	
	public String css(String name) {
		if (name == null) throw new NullPointerException("name must not be null.");
		
		String style = attr("style");
		if (style == null || style.isEmpty()) return null;
		
		Matcher m = STYLE_PATTERN.matcher(style);
		while (m.find()) {
			if (name.equalsIgnoreCase(m.group(1))) {
				return m.group(2);
			}
		}
		return null;
	}
	
	public Nodes css(String name, String value) {
		if (name == null) throw new NullPointerException("name must not be null.");
		
		String style = attr("style");
		if (style != null && !style.isEmpty()) {
			Map<String, String> map = new LinkedHashMap<String, String>();
			
			Matcher m = STYLE_PATTERN.matcher(style);
			while (m.find()) {
				if (m.group(1) == null || m.group(1).isEmpty()) continue;
				
				String cname = m.group(1).toLowerCase();
				String cvalue = (m.group(2) != null && !m.group(2).isEmpty()) ? m.group(2) : null;
				map.put(cname, cvalue);
			}
			
			name = name.toLowerCase();
			value = (value != null && !value.isEmpty()) ? value : null;
			map.put(name, value);
			
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				if (entry.getValue() == null) continue;
				sb.append(entry.getKey()).append(':').append(entry.getValue()).append(';');
			}
			if (sb.length() > 0) {
				attr("style", sb.toString());
			} else {
				removeAttr("style");
			}
		} else {
			name = name.toLowerCase();
			value = (value != null && !value.isEmpty()) ? value : null;
			if (value != null) {
				attr("style", name + ":" + value + ";");
			} else {
				removeAttr("style");
			}
		}
		return this;
	}
	
	public Nodes css(Map<String, String> props) {
		if (props == null || props.isEmpty()) return this;

		String style = attr("style");
		if (style != null && !style.isEmpty()) {
			Map<String, String> map = new LinkedHashMap<String, String>();
			Matcher m = STYLE_PATTERN.matcher(style);
			while (m.find()) {
				if (m.group(1) == null || m.group(1).isEmpty()) continue;
				
				String cname = m.group(1).toLowerCase();
				String cvalue = (m.group(2) != null && !m.group(2).isEmpty()) ? m.group(2) : null;
				map.put(cname, cvalue);
			}
			
			for (Map.Entry<String, String> entry : props.entrySet()) {
				if (entry.getKey() == null || entry.getKey().isEmpty()) continue;
				
				String name = entry.getKey().toLowerCase();
				String value = (entry.getValue() != null && !entry.getValue().isEmpty()) ? entry.getValue() : null;
				map.put(name, value);
			}

			
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				if (entry.getValue() == null) continue;
				sb.append(entry.getKey()).append(':');
				sb.append(entry.getValue()).append(';');
			}
			if (sb.length() > 0) {
				attr("style", sb.toString());
			} else {
				removeAttr("style");
			}
		} else {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> entry : props.entrySet()) {
				if (entry.getKey() == null || entry.getKey().isEmpty()) continue;
				if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
				sb.append(entry.getKey().toLowerCase()).append(':');
				sb.append(entry.getValue()).append(';');
			}
			attr("style", sb.toString());
		}
		
		return this;
	}
	
	/**
	 * Gets count of current nodes.
	 * 
	 * @return count of current nodes
	 */
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
	
	static int toFilter(MatchType type) {
		switch (type) {
		case DOCUMENT_NODE:
			return NodeFilter.SHOW_DOCUMENT;
		case DOCUMENT_TYPE_NODE:
			return NodeFilter.SHOW_DOCUMENT_TYPE;
		case ELEMENT_NODE:
		case ATTRIBUTE_NODE:
			return NodeFilter.SHOW_ELEMENT;
		case TEXT_NODE:
			return NodeFilter.SHOW_TEXT;
		case COMMENT_NODE:
			return NodeFilter.SHOW_COMMENT;
		case PROCESSING_INSTRUCTION_NODE:
			return NodeFilter.SHOW_PROCESSING_INSTRUCTION;
		default:
			return NodeFilter.SHOW_ALL;
		}		
	}
}
