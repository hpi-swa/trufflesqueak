as yet unclassified
testAddCodeBoxesToSnippetFolder1

	| codeBoxFile codeFile rootDirectory snippetsDirectory code attribute codeBoxesResult codeResult tmp |
	codeFile := PEFileEntryTestFile new
		fullPath: '/tmp/code.js';
		content: '
let x = 6;

# CODE BOX BEGIN:./snippets/codeBox.js
Polyglot.export("x", x);
Polyglot.evalFile("js", path="/tmp/snippets/codebox.js");
const y = Polyglot.import("y");
# CODE BOX END

console.log(y);' withUnixLineEndings
		yourself.
	codeBoxFile := PEFileEntryTestFile new
		fullPath: '/tmp/snippets/codeBox.js';
		content: '
x = Polyglot.import("x")
# CODE BEGIN:{"boxName":"Code Box","importVariables":["x"],"language":"js","exportVariables":["y"]}
let y = 6;
y += x;
# CODE END
Polyglot.export("y", y)' withUnixLineEndings
		yourself.
	snippetsDirectory := PEFileEntryTestDirectory new
		fullPath: '/tmp/snippets';
		children: {codeBoxFile};
		yourself.
	rootDirectory := PEFileEntryTestDirectory new
		fullPath: '/tmp';
		children: {codeFile. snippetsDirectory};
		yourself.
	codeFile parent: rootDirectory.
	codeBoxFile parent: snippetsDirectory.
	
	
	attribute := TextColor new.
	code := codeFile read asText.
	code addAttribute: attribute from: 1 to: 10.
	
	tmp := PECodeBoxReadWriter addCodeBoxesTo: code file: codeFile.
	codeResult := tmp first.
	codeBoxesResult := tmp second.
	
	self assert: ('
let x = 6;

', (String value: 1), '

console.log(y);') withUnixLineEndings equals: codeResult asString.

	"Verify that our text attribute is still there."
	self assert: attribute equals: (codeResult attributesAt: 1) first.
	
	self assertCodeBoxAt: 14 in: codeResult.
	self assert: 1 equals: codeBoxesResult size.
	self assert: (codeBoxesResult first isKindOf: PECodeBox)