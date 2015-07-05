# XMLIC - jQuery like DOM traversal and manipulation API

## XMLIC（ズムリック）とは

jQuery ライクな DOM 操作を実現する Java 用 XML APIです。JDOM や XOM などと異なり、あくまで W3C 標準の DOM に対し検索、操作を行なうため、XSLT など他ライブラリとの高い相互運用性を実現出来ます。API は jQuery に似ていますが、XMLIC は XML の操作に特化しているため、次のような違いがあります。

- CSS セレクタの代わりに XPath 1.0 および XPath パターンを使用します。
- 複数ドキュメントを扱うことができます。入出力機能も用意されています。
- XML名前空間を適切に扱うことができます。

使い方は簡単です。jQuery 利用者ならば、すぐに使いはじめることができるでしょう。

```java
import net.arnx.xmlic.XML;

// test.xml を読み込んで div タグに class="alert" という属性を設定します。
XML.load(new File("test.xml"))
    .find("div")
    .attr("class", "alert");
```
## 基本的な使い方

jQuery の Traversal/Manipulation API をそのままの仕様で移植していますので同じように操作が可能です。大きく違う点は、Document の取り回しです。 jQuery が単一の window.document に対して　XMLIC は複数のドキュメントを扱う必要があります。このような処理のためにXML クラスが用意されています（jQuery の $ 関数に当たるものだと考えるとわかりやすいでしょう）。

- 読み込みには、 XML クラスの load() スタティックメソッドを使います。
- 読み込んだ XML インスタンスに対し、find() メソッドで Nodes インスタンスを取得し操作します（Nodes インスタンスは jQuery オブジェクトに当たるものだと考えるとよいでしょう）。
- XML インスタンス は、Nodes インスタンスの getOwner() メソッドを通じて取得できます。
- 書き込みは、 XML インスタンスの writeTo() メソッドを使います。

XPath は XSLT 1.0 パターンとして判定されます。そのため、子孫要素の div を捜すために find(".//div") のように記述する必要はなく、 find("div") と書くことができます。

## XML 文書の読み込み - Load

XML 文書の読み込みには、２パターンが用意されています。通常は XML クラスの load スタティックメソッドを使います。オプションは指定出来ませんが、通常の利用では困ることはないでしょう。

```java
// ファイルから XML クラスのインスタンスを取得します。
XML xml = XML.load(new File("test.xml"));

// InputStream から XML クラスのインスタンスを取得します。クローズは自動的に行われます。
XML xml = XML.load(new FileInputStream("test.xml"));

// Reader から XML クラスのインスタンスを取得します。クローズは自動的に行われます。
XML xml = XML.load(new FileReader("test.xml"));

// URI から XML クラスのインスタンスを取得します。
XML xml = XML.load(new URI("http://..."));
```

検証を行なう必要があるなど、オプションの指定が必要な場合は、XMLLoader （あるいは DocumentBuilder などを通じて）構築した Document オブジェクトを XML クラスのコンストラクタを使ってラップします。

```java
// XMLLoader から DOM を構築し、XML クラスでラップします。
XMLLoader loader = new XMLLoader();
loader.setValidation(true);
loader.setIgnoringComments(true);
XML xml = loader.load(new FileInputStream(new File("test.xml")));
```

外部にあるドキュメントではなく、部分的な XML や DOM ノード値して構築されている場合は、 Nodes　コンストラクタを使って取り込みます。

```java
// 部分的な XML をインラインで読み込みます。
Nodes nodes = new Nodes(xml, "<div>部分的なXML</div><div>部分的なXML</div>");

// 構築済みのノードを Nodes オブジェクトに変換します。
Node node = ...;
Nodes nodes = new Nodes(xml, node);

NodeList list = ...;
Nodes nodes = new Nodes(xml, list);
```

## DOM の検索 - Traversal

### 要素の検索 - find

要素の検索には find メソッドを使います。find メソッドは、XML クラス、Nodes クラスの両方で利用できます。引数には、XPath 式を記述します。

```java
// ドキュメントから div 要素を探し、さらに href 属性をもつ a 子要素を検索します。
xml.find("div").find("a[@href]");
```

名前空間に対して検索する場合は、プレフィックスを付与します。デフォルトでルート要素に付与された名前空間情報は収集しますが、読み込んだドキュメントに依存するため、明示的に指定する方がよいでしょう。

```java
// XML インスタンスを構築し、利用する名前空間を指定します。
XML xml = XML.load(new File("test.xml"));
xml.addNamespaceMapping("h", "http://www.w3.org/1999/xhtml");

// ドキュメントから xhtml の div 要素を探し、さらに href 属性をもつ xhtml の a 子要素を検索します。
// 指定するプレフィックスは、ドキュメント内に実際に付与されたプレフィックスには依存しません。
xml.find("h:div").find("h:a[@href]");
```

### ノードの検索 - select

XPath は、要素以外にもテキストノードや属性ノード、コメントノードなども対象にすることができます。 XMLIC では、それに対応して jQuery にはない select メソッドを追加しています。select メソッドは、XML クラス、Nodes クラスの両方で利用できます。引数には、XPath 式を記述します。属性値の設定／取得には val メソッドを使います。

```java
// ドキュメントから最初の属性ノードを検索し、その値を取得します。
xml.select("attribute::node()[1]").val();
```

jQuery では val メソッドは、入力タグに対する値を取得するメソッドとして用意されていますが、 XMLIC では、Node クラスの get/setNodeValue() に対するアクセッサとして機能します。

### ノードの横断 - traverse

DOM Level2 では、ノードを横断し列挙する機能が追加されましたが、XMLIC　にも対応する機能が用意されています。

```java
// ドキュメントからすべての要素にマッチするノードを順番に訪問します。
xml.traverse("*", new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
  	System.out.println(current.name());
  }
});

// 第3引数を true にすることで逆順にたどることもできます
xml.traverse("*", new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
  	System.out.println(current.name());
  }
}, true);
```

### XPath と CSS セレクタとの比較

主要な XPath 式と findなどで用いるXPath パターンとCSS セレクタの対比表を次に示します。参考にしてください。

|対象          |XPath                                 |XPath パターン                  |CSS セレクタ        |
|--------------|--------------------------------------|--------------------------------|--------------------|
|すべての要素  |.//\*                                 |\*                              |\*                  |
|要素が一致    |.//element                            |element                         |element             |
|子要素        |.//parent/child                       |parent/child                    |parent > child      |
|子孫要素      |.//ancestor//descendant               |ancestor//descendant            |ancestor descendant |
|属性がある    |.//\*[\@name]                         |\*[\@name]                      |[name]              |
|属性が一致    |.//\*[\@name='value']                 |\@name='value'                  |[name='value']      |
|n番目の子要素 |.//\*[n] あるいは .//\*[position()=n] |\*[n] あるいは \*[position()=n] |:nth-child(n)       |
|最初の子要素  |.//\*[1]                              |\*[1]                           |:first-child        |
|最後の子要素  |.//\*[last()]                         |\*[last()]                      |:last-child         |
|OR 条件選択   |expr \| ... \| expr                   |expr \| ... \| expr             |expr, ..., expr     |

jQueryでは #ID の形でID指定を多用しますが、XMLではスキーマにID型が指定されない限り利用できないため、通常の属性同様 @id='ID' と記述します （XPath にも ID 構文は存在しますが、スキーマを使い検証を実施し DOM 内に ID 型であるという情報が設定されている場合に限り、id()='ID' と書くことで検索が可能です）。

### 子要素の取得 - children

子要素の取得には、children メソッドを使います。引数に XPathのフィルタ条件（角カッコの中）を記載することでフィルタリングが可能です。

```java
// ドキュメントの中から div 要素を検索し、それらの要素に対する子要素の一覧を取得します。
xml.find("div").children();

// ドキュメントの中から div 要素を検索し、span という名前を持つその子要素の一覧を取得します。
xml.find("div").children("span");
```

### 親要素の取得 - parent, parents / parentsUntil, closest

親要素の取得には、parent メソッドを使います。すべての親（＝先祖）要素を取得する場合は parents メソッドを使います。 引数に XPathのフィルタ条件（角カッコの中）を記載することでフィルタリングが可能です。

```java
// div 要素を検索し、それらの要素に対する親要素を取得します。
xml.find("div").parent();

// div 要素を検索し、それらの要素にすべての親要素を取得します。
xml.find("div").parents();

// フィルタリングも可能です。
xml.find("div").parent("@class='test'");
xml.find("div").parents("@class='test'");
```

条件に一致する親要素までを取得できる parentsUntil、自分も含むすべての親要素をから最初に条件に一致した要素を取得できる closest も同様に用意されています。

```java
//  div 要素を検索し、class="test" を持つ要素までの親要素を取得します。
xml.find("div").parentsUntil("@class='test'");

//  div 要素の自分も含む上位要素の中から、最初に条件に一致する要素を取得します。
xml.find("div").closest("@class='test'");
```

### 兄弟要素の取得 - prev / prevUntil / prevAll, next / nextUntil / nextAll, siblings

兄弟要素の取得には、前にある要素を取得する prev 系メソッド、後にある要素を取得する next 系メソッド、前後両方の要素を取得する siblings メソッドがあります。

prev 系メソッドには、直前を取得する同名メソッドの他に、前に位置する兄弟要素すべてを取得する prevAll、 条件に一致するまで前にさかのぼる prevUntil があります。

```java
// div 要素を検索し、それらの要素に対する直前の要素の一覧を取得します。
xml.find("div").prev();

// div 要素を検索し、それらの要素に対して前にある要素すべての一覧を取得します。
xml.find("div").prevAll();

// div 要素を検索し、それらの要素に対して条件に一致するまで前にある要素を取得します。
xml.find("div").prevUntil("@class='test'");

// フィルタリングも可能です。
xml.find("div").prev("@class='test'");
xml.find("div").prevAll("@class='test'");
```

next 系メソッドも同様に、直後を取得する同名メソッドの他に、後ろに位置する兄弟要素すべてを取得する nextAll、 条件に一致するまで後ろに進む nextUntil があります。

```java
// div 要素を検索し、それらの要素に対する直後の要素の一覧を取得します。
xml.find("div").next();

// div 要素を検索し、それらの要素に対して後ろにある要素すべての一覧を取得します。
xml.find("div").nextAll();

// div 要素を検索し、それらの要素に対して条件に一致するまで後ろにある要素を取得します。
xml.find("div").nextUntil("@class='test'");

// フィルタリングも可能です。
xml.find("div").next("@class='test'");
xml.find("div").nextAll("@class='test'");
```

siblingsは、すべての兄弟要素を取得するメソッドで prevAll と nextAll を結合したような効果を持ちます。

```java
// div 要素を検索し、それらの要素に対するすべての兄弟要素を取得します。
xml.find("div").siblings();

// フィルタリングも可能です。
xml.find("div").siblings("@class='test'");
```

### 子ノードの取得 - contents

子要素を取得するには、children メソッドを使いますが、テキストノードなどは除外されます。テキストノードやコメントノードなどすべての子ノードを取得する場合には、 contents メソッドを使います。

```java
// div 要素を検索し、それらの要素に対するすべての子ノードを取得します。
xml.find("div").contents();

// フィルタリングも可能です。
xml.find("div").contents("first()");
```

### 取得結果の操作 - filter / not, eq　/ first / last　/ slice

取得結果をフィルタリングしたい場合は、filter メソッドを使います。filterメソッドには、XPath 式によるフィルタと、内部クラスを用いた２種類が用意されています。not メソッドは、filter とは逆に条件にマッチしないものだけを残します。

```java
// div 要素を検索した結果に対し属性でフィルタリングします。
xml.find("div").filter("@name='test'");

// div 要素を検索した結果に対し条件にマッチしないものだけ残します。
xml.find("div").not("@name='test'");

// div 要素を検索した結果に対しメソッドを使って属性ででフィルタリングします。
xml.find("div").filter(new Visitor() {
	public boolean visit(Node node) {
		return "test".equals(node.getAttributeNS(null, "name"));
	}
});
```

また、位置によるフィルタリングとして eq、first、lastの３メソッドが、位置範囲に 対するフィルタとして slice が用意されています。なお、first、last メソッドは、 それぞれ eq(0)、eq(-1) のショートカットです。

```java
// div 要素を検索した結果の3番目を取得します。
xml.find("div").eq(3);

// div 要素を検索した結果の2～4番目を取得します。
xml.find("div").slice(2, 4);
```

jQuery の :first、:last とは異なり、XMLIC には結果集合それ自体に対して フィルタリングする XPath 式がないがめ、常にfirst()、last()を使う必要があることに注意して ください。

### 取得結果の結合 - add, addBack

取得結果に対し、別の検索結果を結合したい場合は、add メソッドを使います。一つ前の処理結果を現在の結果に結合したい場合は、 addBack　を使います。

```java
// div 要素を検索した結果に p 要素を検索した結果を結合します。なお、次の二つの表現は等価です。
xml.find("div").add("p");
xml.find("div").add(xml.find("p"));

// div 要素の子要素  p を検索した結果に 最初の div 要素の検索結果を結合します。フィルタリングも可能です。
xml.find("div").find("p").addBack();
```

### 取得結果の復元 - end

一つ前の取得結果に戻したい場合は、 end メソッドを使います。このメソッドを使うことでメソッド連結でも入れ子表現が可能になります。

```java
// div 要素を検索した結果に p 要素を検索した結果を結合した後、最初の検索結果を取得します。
xml.find("div")
  .find("p")
.end();
xml.find("div").find("p").addBack("@name='test'");
```

### 取得結果の列挙 - each

Nodes クラスは、ArrayList<Node> を継承しているため、通常のリストと同様、for 文による列挙が可能です。

```java
// 取得結果を列挙します。
for (Node node : xml.find("div")) {
  Nodes current = new Nodes(xml, node);
  System.out.println(current.name());
}
```
jQuery 同様に each メソッドによる列挙も可能です。ただし、 jQuery とはシンタックスが異なる点に注意が必要です。

```java
// 取得結果を列挙します。
xml.find("div").each(new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
    System.out.println("" + status.getIndex() + ": " + current.name());
  }
});

// 第ニ引数に true を設定することで、逆順の列挙も可能です。
xml.find("div").each(new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
    System.out.println("" + status.getIndex() + ": " + current.name());
  }
}, true);

// 途中で break したい場合は cancel メソッドを使ってください。キャンセル例外が発生して処理を中断します。
xml.find("div").each(new Visitor<Nodes>() {
  public void visit(Nodes current, Status status) {
    if (status.getIndex() == 3) status.cancel();
    System.out.println("" + status.getIndex() + ": " + current.name());
  }
});
```

## DOM の操作 - Manipulation

###名前の操作 - name, namespace, prefix, localName

XML と HTML の大きな違いとして名前空間のサポートがあります。仕様としては自然なものですが XML の利用において最大の難物とも言えます。 まず、XML における名前の概念を整理すると次のようになります。なお、属性については、プレフィックスを付けない場合、名前空間はデフォルト名前空間 ではなく、要素の名前空間に属します。

|概念                    |説明                                 |例                           |
|------------------------|-------------------------------------|-----------------------------|
|名前空間(Namespace URI) |タグセットの仕様を表す URI           |http://www.w3.org/1999/xhtml |
|プレフィックス(Prefix)  |文書中での名前空間に対する短縮名     |任意（hなど）                |
|ローカル名(Local Name)  |タグの要素や属性の名前               |div、span など               |
|修飾された名前（QName） |プレフィックスで修飾されたローカル名 |h:div、h:span など           |

名前空間導入以前の XML における名前やいわゆる タグ名（tagName）は、デフォルト名前空間で修飾された名前として扱われます。

jQuery には名前の操作機能がありませんが、XMLIC では、名前空間、プレフィックス、ローカル名、修飾された名前それぞれに対し namespace、prefix、localName、name メソッドでアクセスすることができます（複数のノードが含まれる場合は、先頭ノードの情報が返されます）。

```java
Nodes nodes = xml.parse("<h:div xmlns:h=\"http://www.w3.org/1999/xhtml\">テスト</h:div>");
nodes.namespace(); // http://www.w3.org/1999/xhtml
nodes.prefix(); // h
nodes.localName(); // div
nodes.name(); // h:div
```

変更する場合も、引数に値を設定するだけです。

```java
Nodes nodes = xml.parse("<h:div xmlns:h=\"http://www.w3.org/1999/xhtml\">テスト</h:div>");
// プレフィックスは、文書中直近に指定されているものになります。存在しないときのみ、引数で指定したプレフィックスとなります。
nodes.namespace("http://www.w3.org/2000/svg");　// デフォルトに戻す場合は、null を設定します。
nodes.prefix("svg"); // プレフィックスのみを変更します（名前空間は変わりません）
nodes.localName("span"); // ローカル名のみを変更します（名前空間は変わりません）

// XML 構築時に設定したプレフィクスで指定します。実際のプレフィックスは、文書中直近に指定されているものになります。
// 存在しないときのみ、引数で指定したプレフィックスとなります。
nodes.name("svg:span"); // svg　に紐づく名前空間と span というローカル名に変更されます。
```

名前空間については removeNamespace メソッドを使うことで削除することも可能です。

```java
Nodes nodes = xml.parse("<h:div xmlns:h=\"http://www.w3.org/1999/xhtml\">テスト</h:div>");
// ネームスペースを削除します。
nodes.removeNamespace();

// 特定のネームスペースだけを削除することもできます。
nodes.removeNamespace("http://www.w3.org/1999/xhtml");
```

### 属性の操作

属性の操作は、attr メソッドを通じて行います。このメソッドも、名前同様に名前空間を考慮して操作することが可能です。

```java
nodes.attr("name"); // 属性の値を取得します。
nodes.attr("name", "1") // 属性の値を設定します。

// 名前空間の取り扱いも可能です。XML 構築時に設定したプレフィクスで指定します。
nodes.attr("http:name"); // 名前空間付きの属性の値を取得します。

// XML 構築時に設定したプレフィクスで指定します。属性が追加される場合、実際のプレフィックスは、文書中直近に指定されているものになります。
// 存在しないときのみ、引数で指定したプレフィックスとなります。
nodes.attr("http:name", "1") // 名前空間を使って、属性を設定します。
```

### テキスト／値の操作

要素の子要素となっているテキストの操作には、text メソッドを使用します。

```java
// 子ノード（以下）に含まれるテキストを結合して返されます。
nodes.text();

// 子ノードをテキストで置換します。
nodes.text("text");
```

XPath では、要素以外のノードについても検索が可能なため、属性やコメントなど要素以外のノード値を操作する方法が用意されています。 具体的には val メソッドを使います。jQuery にも val メソッドがありますが、異なる動作をしますので 注意してください（将来的には、input など同名の要素に対して同じ動作を実装することも検討しています。現在は要素に対しては何の動作もしません）。

```java
// div　要素以下の最初のノードの値を取得します。
xml.find("div").contents().val();

// div　要素以下の各種ノードの値を設定します。
xml.find("div").contents().val("text");
```

### 要素の評価 - is, index

要素が条件に一致するか調べたい場合には、is メソッドを使います。条件に一致するインデックス番号を取得したい場合には、indexメソッドを使います。

```java
// すべての div 要素に属性 name="test"が存在する場合 true になります。
boolean result = xml.find("div").is("@name='test'");

// div で最初に条件が一致したインデックス番号を返します。 見つからない場合は -1 を返します。
int index = xml.find("div").index("@name='test'");

// フィルタの代わりに、Node を指定することもできます。
Node node = xml.find('div').first().get(0);
boolean result = xml.find("div").is(node);
int index = xml.find("div").index(node);
```

### 要素の追加 - prepend / prependTo, append / appendTo, before / insertBefore, after / insertAfter

要素を追加したい場合は、prepend系、append系、before系、after 系の各メソッドを使います。 これらのメソッドは、それぞれ挿入位置が違うだけで、同じような使い方ができます。

prepend、append、before、after と prependTo、appendTo、insertBefore、insertAfter の違いは、前者がオブジェクトの要素群に引数で指定した要素群を加えるのに対し、後者は、引数で指定した要素群にオブジェクトの要素群を加える点にあります（対象が逆になる）。

```java
// すべての div 要素に指定した要素を追加します。
xml.find("div").prepend("<span>最初の子要素として追加！</span>");
xml.find("div").append("<span>最後の子要素として追加！</span>");
xml.find("div").before("<span>直前の要素として追加！</span>");
xml.find("div").after("<span>直後の要素として追加！</span>");

// 同じ結果になりますが記述が逆転します。
xml.parse("<span>最初の子要素として追加！</span>").prependTo("div");
xml.parse("<span>最後の子要素として追加！</span>").appendTo("div");
xml.parse("<span>直前の要素として追加！</span>").insertBefore("div");
xml.parse("<span>直後の要素として追加！</span>").insertAfter("div");
```

### 要素の置換 - replaceWith / replaceAll

要素の置換には、replaceWith メソッドと replaceAll メソッドを使います。ふたつのメソッドの違いは、要素の追加と同様に置換対象と置換内容の記述位置だけです。

```java
// すべての div 要素を指定した要素で置換します。
xml.find("div").replaceWith("<span>最初の子要素として追加！</span>");

// 同じ結果になりますが記述が逆転します。
xml.parse("<span>最初の子要素として追加！</span>").replaceAll("//div");
```

### 要素のラッピング - wrap / wrapInner / wrapAll / unwrap

要素のラッピングには、wrap 系メソッドを使います。wrap は要素自身を、wrapInner は要素の内側をラッピングします。 unwrap は wrap とは反対にラッピングを解除し、子要素で置換します。

```java
// すべての div 要素を指定した要素でラップします。
xml.find("div").wrap("<div class=".wrap"></div>"); // ><の間に要素が挿入されます。
xml.find("div").unwrap(); // すべての div 要素のラッピングを解除します。

// すべての div 要素の内側を指定した要素でラップします。
xml.find("div").wrapInner("<div class=".wrap"></div>");
wrapAll は、対象となる要素の最初の位置に対象のすべての要素を包み込むようにラッピングします。

// 最初の div 要素の位置にすべての div 要素がラッピングされるように移動されます。
xml.find("div").wrapInner("<div class=".wrap"></div>");
```

### 要素の削除 - remove, empty

要素を削除するメソッドとしては、要素自身を含めて削除する remove メソッドと要素の内側を削除する empty メソッドがあります。

```java
// すべての div 要素を削除します。
xml.find("div").remove();
xml.find("div").remove("span"); // マッチした要素だけを削除することも可能です。

// すべての div 要素を空（＝内側を削除）にします。
xml.find("div").empty();
```

### ノードの複製 - clone

ノードを複製したい場合は、clone メソッドを使用します。

```java
Nodes clone = xml.find("div").clone();
```

### ノードの評価 - evaluate

XPath では、ノードの選択の他に文字列/数値/真偽値の結果を返す場合があります。XMLIC には、そのような場合にも対応できるように、evaluate という汎用の XPath 評価メソッドを用意しています。引数には、 xpath と戻り値の型を指定します。型には、Nodes、NodeList、 Node、String、Boolean / boolean、各種数値型（Numberのサブクラス） を指定することができます。

```java
if (nodes.evaluate("self::node()[@name='test']", boolean.class)) {
  // マッチした！
}
```

### ノードの正規化 - normalize

normalize メソッドは、ノードの正規化を行います。具体的には、子要素より下にあるテキストノードを結合してひとつのノードにまとめます。これは、Node クラスの normalize の動作と同じですが、XMLIC ではこの動作に加え、不用な名前空間宣言の除去も行います。

```java
nodes.normalize();
```

### データの紐付け - data/removeData

DOM の属性などにはしたくないが、特定のノードとオブジェクトを紐付けておきたい場合には Data APIを利用します。

```java
// ノードにオブジェクトを紐付けます。
nodes.data("key", "value");

// ノードからオブジェクトを取得します。
Object value = nodes.data("key");

// ノードからオブジェクトを削除します。
nodes.removeData("key");
```

なお、jQuery同様「data-名前」という属性がある場合には、Data API から取得できるデフォルト値として扱います。

### HTML互換機能 - addClass/toggleClass/removeClass/hasClass, css

XMLIC は XML を対象としていますが、XHTML での操作時に便利なように次の jQuery ライクな HTML 互換操作機能を用意しています。

- addClass、removeClass はそれぞれ要素の class 属性の空白文字で区切られたリストに文字列を追加／削除します。
- toggleClass は、フラグにより要素の追加、削除を入れ替えます。
- hasClass は、要素の class 属性の空白文字で区切られたリストに指定した文字列が含まれているかを確認します。

```java
// クラスを追加します。複数追加するときは空白で区切ります。
nodes.addClass("warning error");

// クラスを削除します
nodes.removeClass("warning");

// クラスを入れ替えます。
nodes.removeClass("warning", warning != null);

// クラスが存在しているかチェックします。
if (nodes.hasClass("error")) {
    System.out.println("エラーはまだ存在しています。");
}
```

css メソッドは、要素の style 属性に対し CSSスタイルを取得／追加／削除します。jQuery とは異なり、link要素やstyle要素で指定されたスタイル情報ができるわけではないので、注意してください。

## XML 文書の出力 - Write

XML 文書の出力も読み込み同様に２パターンが用意されています。通常は XML クラスの writeTo メソッドを使います。オプションは指定出来ませんが、通常の利用には十分でしょう。

```java
// ファイルに  DOM の内容を出力します。
xml.writeTo(new File("test.xml"));

// OutputStream に  DOM の内容を出力します。クローズは自動的に行われます。
xml.writeTo(new FileOutputStream("test.xml"));

// Writer に DOM の内容を出力します。クローズは自動的に行われます。
xml.writeTo(new OutputStreamWriter(new FileOutputStream("test.xml"), "Windows-31J"));
```

改行コードの変更などオプションの指定が必要な場合は、XMLWriter を使います。

```java
// DOM を XMLWrite に渡しファイルに出力します。
XMLWriter writer = new XMLWriter();
writer.setEncoding("EUC-JP");
writer.setLineSeparator("\r\n");
writer.setPrettyPrinting(true);

writer.writeTo(new FileOutputStream("test.xml"), xml);
```

部分的な XML を出力したい場合は、toString でも取得出来ます。

## Mavenリポジトリ

XMLIC は、0.9.1 以降 Maven Central Repository に登録されるようになりました。groupId、artifactIdは次の通りです。

```xml
<groupId>net.arnx</groupId>
<artifactId>xmlic</artifactId>
```

## ライセンス
XMLIC は、Apache License, Version 2.0下で配布します。

自分のライブラリへの組み込んでいただいたり、その際にパッケージ名の変更や処理の変更など行っていただいても何ら構いません。ライセンスの範囲内でご自由にお使いください。

なお、 XMLIC は jaxen をパッケージを変更し同梱しています。その部分に関しては jaxen のライセンスに従ってください（Apache ライセンスに準じた緩いライセンスとなっていますので、特別な考慮は必要はありません）。

## リリースノート

### 2015/7/4 version 1.0.3

- すでに root 要素が存在してる XML オブジェクトに root メソッドで要素を設定するとエラーが発生する問題を修正しました。
- Nods クラスに JavaScript 互換の sort() メソッドを追加しました。

### 2014/3/1 version 1.0.2

- Nodes クラスに JavaScript 互換の reverse() メソッドを追加しました。
- Nodes クラスに jQuery 同様の addClass/toggleClass/removeClass/hasClass, css を追加しました。

### 2013/11/8 version 1.0.1

- Nodes クラスに owner() メソッドを追加するとともに、getOwner() メソッドを非推奨にしました 。
- Nodes クラスに jQuery 同様の Data API を追加しました。
- Nodes クラスにて別のドキュメントの要素を append/prepend/before/after した際、エラーが発生していた問題を修正しました。

### 2013/9/18 version 1.0.0

- XML や XSLT のロード時に発生する例外を XMLException に統一しました。個々のエラーにもアクセスすることができます。XML　や XMLException の getWarnings() メソッドを通じて警告にもアクセスできます。
- XSLT を取り扱う XSLT クラスを追加しました。
- XMLLoader#load の戻り値を Document から XML クラスに変更しました。
- XMLWriter#writeTo に XML クラスを引数にとるメソッドを追加しました。
- XML#parse を廃止し、 new Nodes(XML, String) コンストラクタに変更しました。

### 2013/8/25 version 0.9.2

- ロード時の名前空間走査処理をルートノードだけに限定しました。
- デフォルト名前空間が指定された場合の XPath 式の取り扱いを XSLT2.0 の xpath-default-namespace 属性設定に合わせました。
- HTML 互換機能として addClass, css メソッドを追加しました。
- 各クラス／メソッドにコメントを追加しました。
- StatusImpl を internal パッケージに移動しました。

### 2013/8/17 version 0.9.1

- each、filter など繰り返し処理の引数インターフェイスを Java で利用しやすいように見直しました。
- DOM2 Traversal API に対応した traverse メソッドを追加しました。

### 2013/8/8 version 0.9.0

- XPath 式として XSLT 1.0 パターンを使うように変更しました。これにより .// のような記述が必要なくなり、より jQuery に近い書き方が可能になりました。 ただし、 select/evaluate は従来通り通常の XPath として解釈します。
- 空のドキュメントに append できない不具合を修正しました。また、XMLクラスに XML文字列を指定できるコンストラクタを追加しました。
- XMLエスケープ用に XML.escape(str)/XML.unescape(str) を追加しました。
- XPath 関数として XSLT 同様の動作をする key() 関数を追加しました。

### 2013/8/3 version 0.8.2

- XPath API の動作が導入するライブラリによって結構異なることがわかったので　jaxen を統合しました（jar 内にパッケージを変更して入れたので、他に jar を追加する必要はありません）。
- サンプルで　//foo となっていた部分を .//foo に修正しました。
- Translater は Mapper に名前を変更するとともに、Nodes を取得するように変更しました。
- XML.load で読み込んだ時にネームスペースマッピングを変更する方法がなかった問題を改善しました。
- XPath 関数としてXSLT同様の動作をする document() と current() を追加しました。

### 2013/7/16 version 0.8.1

- 命名や動作が一貫していなかった点をいろいろ修正しました。
- デフォルト名前空間の NamespaceContext での扱いが不適切でしたので修正しました。
- ドキュメントを拡充しました。

### 2013/7/16 version 0.8.0

- XMLIC 最初のリリースとなります。元となる仕様が jQuery API ですので大きな変更は予定していませんが、1.0 までは不具合、要望など積極的に取り入れたいと思いますので、ご要望あれば是非。

Copyright (c) 2013 Hidekatsu Izuno Licensed under the [Apache License 2.0](LICENSE.txt).
