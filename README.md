# XMLIC - jQuery like DOM traversal and manipulation API

## What is XMLIC

Java XML API that realizes jQuery-like DOM manipulation. Unlike JDOM, XOM, etc., since it searches and operates against the W3C standard DOM, high interoperability with other libraries such as XSLT can be realized. The API is similar to jQuery, but since XMLIC is specialized in manipulating XML, there are the following differences.

- Use XPath 1.0 and XPath patterns instead of CSS selectors.
- Multiple documents can be handled. I/O function is also provided.
- XML namespace can be handled appropriately.

Usage is simple. With jQuery users, you can start using it immediately.

```java
import net.arnx.xmlic.XML;

// Read test.xml and set the attribute "class =" alert "to div tag.
XML.load(new File("test.xml"))
    .find("div")
    .attr("class", "alert");
```
## Basic usage

Since jQuery's Traversal / Manipulation API is ported as it is, operation is possible in the same way. The big difference is the handling of Document. For jQuery a single window.document the XMLIC needs to handle multiple documents. For this kind of processing we have an XML class (it's easy to imagine that it is equivalent to jQuery's $ function).

- To load, use the load() static method of the XML class.
- Obtain and manipulate the Nodes instance with the find() method for the imported XML instance (You may think that the Nodes instance is equivalent to a jQuery object).
- An XML instance can be obtained through the getOwner() method of the Nodes instance.
- Write uses the writeTo() method of the XML instance.

XPath is determined as an XSLT 1.0 pattern. Therefore, you do not need to write like find (".// div") to find the descendant div, you can write find ("div").

## Loading a XML document - Load

There are two patterns for loading a XML document. Usually we use load static method of XML class. You can not specify options, but you will not be bothered with normal usage.

```java
// Get instance of XML class from file.
XML xml = XML.load(new File("test.xml"));

// Get an instance of an XML class from an InputStream. Closing will be done automatically.
XML xml = XML.load(new FileInputStream("test.xml"));

// Get an instance of XML class from Reader. Closing will be done automatically.
XML xml = XML.load(new FileReader("test.xml"));

// Get instance of XML class from URI.
XML xml = XML.load(new URI("http://..."));
```

If you need to specify options, such as need to verify, wrap the Document object constructed using XMLLoader (or DocumentBuilder etc.) using the XML class constructor.

```java
// Construct DOM from XMLLoader and wrap it in XML class.
XMLLoader loader = new XMLLoader();
loader.setValidation(true);
loader.setIgnoringComments(true);
XML xml = loader.load(new FileInputStream(new File("test.xml")));
```

If it is built with partial XML or DOM node values rather than external documents, use the Nodes constructor to capture it.

```java
// Read partial XML inline.
Nodes nodes = new Nodes(xml, "<div>partial XML</div><div>partial XML</div>");

// Convert the already constructed nodes to Nodes objects.
Node node = ...;
Nodes nodes = new Nodes(xml, node);

NodeList list = ...;
Nodes nodes = new Nodes(xml, list);
```

## Searching a DOM - Traversal

### Searching elements - find

We use the find method to search for elements. The find method is available for both XML and Nodes classes. In the argument, write an XPath expression.

```java
// It searches the document for div elements and also searches for a child element with href attribute.
xml.find("div").find("a[@href]");
```

When searching for namespace, give a prefix. Namespace information given to the root element by default is collected, but it is better to explicitly specify it because it depends on the imported document.

```java
// Construct an XML instance and specify the namespace to use.
XML xml = XML.load(new File("test.xml"));
xml.addNamespaceMapping("h", "http://www.w3.org/1999/xhtml");

// Finds the div element of xhtml from the document and searches for a child element of xhtml with href attribute.
// The prefix you specify does not depend on the prefix that was actually granted in the document.
xml.find("h:div").find("h:a[@href]");
```

### Searching nodes - select

In addition to elements, XPath can also include text nodes, attribute nodes, comment nodes, and so on. XMLIC adds a select method that does not exist in jQuery correspondingly. The select method is available in both XML class and Nodes class. In the argument, write an XPath expression. Use the val method to set / get the attribute value.

```java
// Search the document for the first attribute node and get its value.
xml.select("attribute::node()[1]").val();
```

In jQuery, the val method is provided as a method to get the value for the input tag, but in XMLIC it functions as an accessor for get / setNodeValue () of the Node class.

### Traversing in a node - traverse

In DOM Level 2, the ability to enumerate across nodes has been added, but features corresponding to XMLIC are also provided.

```java
// We will visit nodes that match all elements sequentially from the document.
xml.traverse("*", new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
  	System.out.println(current.name());
  }
});

// You can follow the reverse order by setting the third argument to true.
xml.traverse("*", new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
  	System.out.println(current.name());
  }
}, true);
```

### Comparison between XPath and CSS selector

Below is a comparison table of XPath patterns and CSS selectors used for major XPath expressions and find etc. please refer.

|Target          |XPath                                   |XPath Pattern                    |CSS Selector        |
|--------------|----------------------------------------|----------------------------------|--------------------|
|All elements  |.//\*                                   |\*                                |\*                  |
|matches a element |.//element                              |element                           |element             |
|child elements       |.//parent/child                         |parent/child                      |parent > child      |
|descendat elements    |.//ancestor//descendant                 |ancestor//descendant              |ancestor descendant |
|has a attribute    |.//\*\[@name]                           |\*\[@name]                        |\[name]             |
|matches a attribute   |.//\*\[@name='value']                   |@name='value'                     |\[name='value']     |
|n-th child of a element |.//\*\[n] or .//\*\[position()=n] |\*\[n] あるいは \*\[position()=n] |:nth-child(n)       |
|first child of element |.//\*\[1]                               |\*\[1]                            |:first-child        |
|last child of element  |.//\*\[last()]                          |\*\[last()]                       |:last-child         |
|or selector  |expr \| ... \| expr                     |expr \| ... \| expr               |expr, ..., expr     |

In jQuery, ID specification is used in the form of #ID in many cases, but as XML can not be used unless an ID type is specified in the schema, it is written as @id='ID' as in the normal attribute (XPath also has ID syntax Although it exists, it is possible to search by writing id()='ID' only when validation is performed using a schema and information of ID type is set in the DOM).

### Getting child elements - children

To retrieve child elements, use the children method. Filtering is possible by describing the XPath filter condition (in square brackets) as an argument.

```java
// Search the div elements from the document and get a list of child elements for those elements.
xml.find("div").children();

// Search the div element from the document and get a list of its child elements named span.
xml.find("div").children("span");
```

### Getting a parent element - parent, parents / parentsUntil, closest

To get the parent element, use the parent method. To get all parent (= ancestor) elements, use the parents method. Filtering is possible by describing the XPath filter condition (in square brackets) as an argument.

```java
// Search the div element and get the parent element for those elements.
xml.find("div").parent();

// Search div elements and get all parent elements for those elements.
xml.find("div").parents();

// Filtering is also possible.
xml.find("div").parent("@class='test'");
xml.find("div").parents("@class='test'");
```

A parentsUntil method that can get up to the parent element that matches the condition, closest that also allows you to first retrieve the element that matches the condition from all parent elements, including itself.

```java
// Search the div element and get the parent element up to the element with class = "test".
xml.find("div").parentsUntil("@class='test'");

// First of all, get the element that matches the condition from among the upper elements including div itself.
xml.find("div").closest("@class='test'");
```

### Getting sibling elements - prev / prevUntil / prevAll, next / nextUntil / nextAll, siblings

To get a sibling element, there are a prev-based method that gets the previous element, a next-based method that obtains the later element, and a siblings method that acquires both front and rear elements.

The prev method has prevAll which gets all the sibling elements located before, prevUntil which goes back to the condition until it matches the condition, in addition to the same name method that gets the previous one.

```java
// Search the div elements and get a list of the previous elements for those elements.
xml.find("div").prev();

// Search div elements and get a list of all previous elements for those elements.
xml.find("div").prevAll();

// Search the div elements and retrieve the previous elements until they match the criteria for those elements.
xml.find("div").prevUntil("@class='test'");

// Filtering is also possible.
xml.find("div").prev("@class='test'");
xml.find("div").prevAll("@class='test'");
```

Similarly for the next type method, there is also a nextAntil that gets all the sibling elements that are located behind, the next name method that gets immediately after, nextUntil that goes backward until it matches the condition.

```java
// Search div elements and get a list of elements immediately following those elements.
xml.find("div").next();

// Search div elements and get a list of all the elements behind those elements.
xml.find("div").nextAll();

// Search the div elements and get the elements behind them until they match the criteria.
xml.find("div").nextUntil("@class='test'");

// Filtering is also possible.
xml.find("div").next("@class='test'");
xml.find("div").nextAll("@class='test'");
```

siblings has the effect of combining prevAll and nextAll with a method that obtains all sibling elements.

```java
// Search div elements and get all the sibling elements for those elements.
xml.find("div").siblings();

// Filtering is also possible.
xml.find("div").siblings("@class='test'");
```

### Getting all children - contents

To retrieve child elements, we use the children method, but exclude text nodes etc. To retrieve all child nodes such as text nodes and comment nodes, use the contents method.

```java
// Search div elements and get all child nodes for those elements.
xml.find("div").contents();

// Filtering is also possible.
xml.find("div").contents("first()");
```

### Operating the getting result - filter / not, eq　/ first / last　/ slice

If you want to filter the acquisition result, use the filter method. There are two kinds of filter methods, XPath expression filter and inner class. The not method leaves only those that do not match the condition, contrary to filter.

```java
// Filter the result of retrieving the div element by the attribute.
xml.find("div").filter("@name='test'");

// Leave only those that do not match the condition for the search result of the div element.
xml.find("div").not("@name='test'");

// Filter on the result of searching for div element by method with method using method.
xml.find("div").filter(new Visitor() {
	public boolean visit(Node node) {
		return "test".equals(node.getAttributeNS(null, "name"));
	}
});
```

In addition, three methods of eq, first and last are prepared as filtering by position, and slice is prepared as a filter for the position range. The first and last methods are shortcuts of eq(0), eq(-1) respectively.

```java
// Get the third result of searching the div element.
xml.find("div").eq(3);

// Get the 2nd to 4th result of retrieving the div element.
xml.find("div").slice(2, 4);
```

Please note that unlike jQuery: first,: last, it is necessary to always use first (), last () for XMLIC because there is no XPath expression to filter on the result set itself.

### Concatenating a getting result - add, addBack

To combine retrieval results with other retrieval results, use the add method. If you want to combine the previous processing result with the current result, use addBack.

```java
// We combine the search results of p elements into the search result of div element. The following two expressions are equivalent.
xml.find("div").add("p");
xml.find("div").add(xml.find("p"));

// Join the search result of the first div element to the search result of the child element p of the div element. Filtering is also possible.
xml.find("div").find("p").addBack();
```

### Restoring a result - end

To return to the previous acquisition result, use the end method. Using this method also enables nested representation in method concatenation.

```java
// After combining the search results of the p element into the search result of the div element, obtain the first search result.
xml.find("div")
  .find("p")
.end();
xml.find("div").find("p").addBack("@name='test'");
```

### Iterating results - each

Since the Nodes class inherits from ArrayList<Node>, enumeration by for statement is possible as with ordinary list.

```java
// List the results.
for (Node node : xml.find("div")) {
  Nodes current = new Nodes(xml, node);
  System.out.println(current.name());
}
```
Like jQuery enumeration with each method is also possible. However, note that the syntax is different from jQuery.

```java
// List the results.
xml.find("div").each(new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
    System.out.println("" + status.getIndex() + ": " + current.name());
  }
});

// Reverse order enumeration is also possible by setting true as the second argument.
xml.find("div").each(new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
    System.out.println("" + status.getIndex() + ": " + current.name());
  }
}, true);

// If you want break in the middle, please use the cancel method. Cancel Exception occurs and processing is interrupted.
xml.find("div").each(new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
    if (status.getIndex() == 3) status.cancel();
    System.out.println("" + status.getIndex() + ": " + current.name());
  }
});
```

## Manipulating a DOM - Manipulation

### Manipulating names - name, namespace, prefix, localName

There is a namespace support as a major difference between XML and HTML. Although it is natural as a specification, it can be said to be the biggest difficulty in using XML. First, the concept of names in XML is organized as follows. For attributes, if you do not add a prefix, the namespace belongs to the element namespace, not the default namespace.

|Term                   |Description                                |Example                          |
|------------------------|-------------------------------------|-----------------------------|
|Namespace URI |URI representing the tag set specification           |http://www.w3.org/1999/xhtml |
|Prefix  |Short name for the namespace in the document     |any （such as h）                |
|Local Name  |Name of tag element or attribute               |div, span ...               |
|QName |Local name qualified with prefix |h:div, h:span ...           |

The names and so-called tag names (tagName) in the XML before the introduction of the namespace are treated as qualified names in the default namespace.

jQuery does not have name manipulation functions, but in XMLIC you can access namespace, prefix, localName, name methods for namespace, prefix, local name, qualified name (including multiple nodes) If it does, information on the first node is returned).

```java
Nodes nodes = xml.parse("<h:div xmlns:h=\"http://www.w3.org/1999/xhtml\">テスト</h:div>");
nodes.namespace(); // http://www.w3.org/1999/xhtml
nodes.prefix(); // h
nodes.localName(); // div
nodes.name(); // h:div
```

To change it, just set a value in the argument.

```java
Nodes nodes = xml.parse("<h:div xmlns:h=\"http://www.w3.org/1999/xhtml\">test</h:div>");
// The prefix is the one specified in the document most recently. It will be the prefix specified by the argument only when it does not exist.
nodes.namespace("http://www.w3.org/2000/svg");　// To return to the default, set it to null.
nodes.prefix("svg"); // Change only the prefix (namespace does not change)
nodes.localName("span"); // Change only the local name (namespace does not change)

// It is specified by the prefix set at the time of XML construction. The actual prefix is the one specified in the document most recently.
// It will be the prefix specified by the argument only when it does not exist.
nodes.name("svg:span"); // It will be changed to svg-based namespace and span local name.
```

For namespaces it is also possible to delete them using the removeNamespace method.

```java
Nodes nodes = xml.parse("<h:div xmlns:h=\"http://www.w3.org/1999/xhtml\">test</h:div>");
// Delete the namespace.
nodes.removeNamespace();

// You can also delete only certain namespaces.
nodes.removeNamespace("http://www.w3.org/1999/xhtml");
```

### Manipulating attributes

Attributes are manipulated through the attr method. This method can also be manipulated by considering namespace as well as name.

```java
nodes.attr("name"); // Gets the value of the attribute.
nodes.attr("name", "1") // Set the value of the attribute.

// Namespace handling is also possible. It is specified by the prefix set at the time of XML construction.
nodes.attr("http:name"); // Gets the value of an attribute with a namespace.

// It is specified by the prefix set at the time of XML construction. When an attribute is added, the actual prefix will be the one specified in the document most recently.
// It will be the prefix specified by the argument only when it does not exist.
nodes.attr("http:name", "1") // Use the namespace to set the attributes.
```

### Text / value manipulation

To manipulate text that is a child element of an element, use the text method.

```java
// It is returned by combining the text contained in the child node (below).
nodes.text();

// Replace the child node with text.
nodes.text("text");
```

In XPath, since it is possible to search for nodes other than elements, there are methods for manipulating node values other than elements such as attributes and comments. Specifically, we use the val method. jQuery also has a val method, but since it behaves differently, please be careful (In the future, we are also considering implementing the same behavior for elements of the same name such as input etc. Currently for elements I will not do anything).

```java
// Get the value of the first node below the div element.
xml.find("div").contents().val();

// div Elements Set the values of the various nodes below.
xml.find("div").contents().val("text");
```

### Evaluating elements - is, index

If you want to check whether an element matches a condition, use the is method. If you want to retrieve the index number that matches the condition, use the index method.

```java
// It is true if the attribute name = "test" exists in all div elements.
boolean result = xml.find("div").is("@name='test'");

// Returns the index number whose condition was first matched by div. If it can not be found, it returns -1.
int index = xml.find("div").index("@name='test'");

// Instead of a filter, you can also specify Node.
Node node = xml.find('div').first().get(0);
boolean result = xml.find("div").is(node);
int index = xml.find("div").index(node);
```

### Adding elements - prepend / prependTo, append / appendTo, before / insertBefore, after / insertAfter

If you want to add an element, use prepend, append, before, and after methods. These methods can be used in the same way, just by their insertion position.

The difference between prepend, append, before, after and prependTo, appendTo, insertBefore, insertAfter is that the former adds the group of elements specified by arguments to the group of elements of the object, while the latter adds the group of elements It is at the point of adding an element group (object is reversed).

```java
// Add the specified element to all div elements.
xml.find("div").prepend("<span>Add as a first child element!</span>");
xml.find("div").append("<span>Add as a last child element!</span>");
xml.find("div").before("<span>Add as a previous element!</span>");
xml.find("div").after("<span>Add as a next element!</span>");

// The result will be the same, but the description will be reversed.
xml.parse("<span>Add as a first child element!</span>").prependTo("div");
xml.parse("<span>Add as a last child element!</span>").appendTo("div");
xml.parse("<span>Add as a previous element!</span>").insertBefore("div");
xml.parse("<span>Add as a next element!</span>").insertAfter("div");
```

### Replacing elements - replaceWith / replaceAll

To replace an element, use the replaceWith method and the replaceAll method. The difference between the two methods is just the description position of the substitution target and the replacement content as well as the addition of the element.

```java
// Replace all div elements with specified elements.
xml.find("div").replaceWith("<span>Add as a first child element!</span>");

// The result will be the same, but the description will be reversed.
xml.parse("<span>Add as a first child element!</span>").replaceAll("//div");
```

### Wrapping elements - wrap / wrapInner / wrapAll / unwrap

For wrapping an element, use the wrap method. wrap wraps the element itself, wrapInner wraps inside the element. Unwrap releases wrapping as opposed to wrap and replaces it with child elements.

```java
// Wrap all div elements with the specified element.
xml.find("div").wrap("<div class=".wrap"></div>"); // An element is inserted between '>' and '<'.
xml.find("div").unwrap(); // Unwrapping all div elements.

// Wrap inside all div elements with the specified element.
xml.find("div").wrapInner("<div class=".wrap"></div>");
// wrapAll wraps all elements of interest in the first position of the target element.

// It is moved so that all div elements are wrapped at the position of the first div element.
xml.find("div").wrapInner("<div class=".wrap"></div>");
```

### Deleting elements - remove, empty

As a method to delete an element, there are a remove method to delete including the element itself and an empty method to delete inside the element.

```java
// Delete all div elements.
xml.find("div").remove();
xml.find("div").remove("span"); // It is also possible to delete only matched elements.

// Make all div elements empty (= delete inside).
xml.find("div").empty();
```

### Clone nodes - clone

If you want to duplicate a node, use the clone method.

```java
Nodes clone = xml.find("div").clone();
```

### Evaluating nodes - evaluate

XPath may return string / numeric / boolean result in addition to node selection. For XMLIC, we have prepared a generic XPath evaluation method called evaluate so that we can deal with such cases. For the argument, specify xpath and return type. For types, you can specify Nodes, NodeList, Node, String, Boolean / boolean, and various numeric types (subclasses of Number).

```java
if (nodes.evaluate("self::node()[@name='test']", boolean.class)) {
  // マッチした！
}
```

### Normalizing a node - normalize

The normalize method performs node normalization. Specifically, we combine text nodes below child elements into one node. This is the same as the normalize behavior of the Node class, but XMLIC does this as well as eliminating unnecessary namespace declarations.

```java
nodes.normalize();
```

### Associating data - data/removeData

I do not want it to be an attribute of DOM, but if I want to associate a specific node with an object, I will use the Data API.

```java
// Link objects to nodes.
nodes.data("key", "value");

// Get an object from a node.
Object value = nodes.data("key");

// Delete the object from the node.
nodes.removeData("key");
```

As with jQuery, if there is an attribute called "data - name", it is treated as a default value that can be obtained from the Data API.

### A Compatible feature of HTML - addClass/toggleClass/removeClass/hasClass, css

Although XMLIC targets XML, we provide the following jQuery-like HTML compatible manipulation function so that it is convenient for operation with XHTML.

- addClass, removeClass adds / removes a character string to / from a list delimited by whitespace character of the element class attribute respectively.
- toggleClass swaps the addition and deletion of elements with flags.
- hasClass checks whether the specified character string is included in the list delimited by whitespace of element's class attribute.

```java
// Add a class. Separate multiple spaces with spaces.
nodes.addClass("warning error");

// Delete the class.
nodes.removeClass("warning");

// Swap the classes.
nodes.removeClass("warning", warning != null);

// Check if the class exists.
if (nodes.hasClass("error")) {
    System.out.println("Error still exists.");
}
```

The css method gets / adds / deletes the CSS style for the style attribute of the element. Unlike jQuery, style information specified by link element and style element is not available, so please be careful.

## Write a XML Document - Write

As well as reading the output of the XML document, 2 patterns are prepared. Usually we use the writeTo method of the XML class. The option can not be specified, but it will be sufficient for normal use.

```java
// Write contents of DOM to file.
xml.writeTo(new File("test.xml"));

// Write contents of DOM to OutputStream. Closing will be done automatically.
xml.writeTo(new FileOutputStream("test.xml"));

// Write contents of DOM to Writer. Closing will be done automatically.
xml.writeTo(new OutputStreamWriter(new FileOutputStream("test.xml"), "Windows-31J"));
```

If you need to specify options such as changing the line feed code, use XMLWriter.

```java
// Pass the DOM to XMLWriter and output it to a file.
XMLWriter writer = new XMLWriter();
writer.setEncoding("EUC-JP");
writer.setLineSeparator("\r\n");
writer.setPrettyPrinting(true);

writer.writeTo(new FileOutputStream("test.xml"), xml);
```

If you want to output partial XML, you can get it by toString.

## Maven Repository

XMLIC is now registered with the Maven Central Repository since 0.9.1. groupId, artifactId are as follows.

```xml
<groupId>net.arnx</groupId>
<artifactId>xmlic</artifactId>
```

## License
XMLIC is distributed under Apache License, Version 2.0.

You do not have to worry about incorporating it in your own library, changing the package name or changing the process at that time. Please use it freely within the license.

In addition, XMLIC has changed jaxen package and shipped. Please follow the license of jaxen for that part (it is a loose license according to Apache license, so special consideration is not necessary).

## Release note

### 2015/7/4 version 1.0.3

- Fixed an issue where setting an element with an root method on an XML object already having a root element causes an error.
- Added JavaScript compatible sort() method to Nods class.

### 2014/3/1 version 1.0.2

- We added JavaScript compatible reverse() method to Nodes class.
- Added addClass / toggleClass / removeClass / hasClass, css similar to jQuery to Nodes class.

### 2013/11/8 version 1.0.1

- I added the owner() method to the Nodes class and deprecated the getOwner () method.
- We added a Data API similar to jQuery to the Nodes class.
- Fixed an issue where an error occurred when append / prepend / before / after elements of another document was changed in Nodes class.

Copyright (c) 2013 Hidekatsu Izuno Licensed under the [Apache License 2.0](LICENSE.txt).
