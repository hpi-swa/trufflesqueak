as yet unclassified
testGetFilesToSaveFromFileIdLineEndings_twoNewCodeboxes

	| code codeFile result codeBox1 codeBox2 rootDirectory |
	codeFile := PEFileEntryTestFile new
		fullPath: '/tmp/code.py';
		yourself.
	rootDirectory := PEFileEntryTestDirectory new
		fullPath: '/tmp';
		children: {codeFile};
		yourself.
	codeFile parent: rootDirectory.

	codeBox2 := PECodeBox new
		boxName: 'Inner Code Box';
		importVariables: {'y'};
		exportVariables: {'z'};
		language: #js;
		code: 'const z = y * 3;' withUnixLineEndings asText;
		yourself.

	codeBox1 := PECodeBox new
		boxName: 'Outer Code Box';
		importVariables: {'x'};
		exportVariables: {'z'};
		language: #js;
		code: (
('const y = 4 + x;', String lf, (String value: 1)) 
			withUnixLineEndings
			asText
			addAttribute: (TextAnchor new
				anchoredMorph: (ToolBuilder build: codeBox2);
				yourself) from: 18 to: 18;
			yourself);
		yourself.
		
	code := ('
x = 4',
String lf, (String value: 1), String lf,
'z') withUnixLineEndings asText.
	code addAttribute: (TextAnchor new
		anchoredMorph: (ToolBuilder build: codeBox1);
		yourself) from: 8 to: 8.

	self withoutAbsolutePathsInSnippets: [result := PECodeBoxReadWriter
		getFilesToSaveFrom: code path: codeFile fullPath id: #python lineEndings: String lf].

	self assert: result size = 3.
	self assert: '
x = 4
# CODE BOX BEGIN:./snippets/Outer Code Box.js
polyglot.export_value(x, "x")
polyglot.eval(language="js", path="./snippets/Outer Code Box.js")
z = polyglot.import_value("z")
# CODE BOX END
z' withUnixLineEndings equals: (result at: codeFile fullPath).

	self assert: 
'var x = Polyglot.import("x");
// CODE BEGIN:{"boxName":"Outer Code Box","exportVariables":["z"],"importVariables":["x"],"language":"js"}
const y = 4 + x;
// CODE BOX BEGIN:./Inner Code Box.js
Polyglot.export("y", y);
Polyglot.evalFile("js", "./Inner Code Box.js");
var z = Polyglot.import("z");
// CODE BOX END
// CODE END
Polyglot.export("z", z);
' withUnixLineEndings equals: (result at: '/tmp/snippets/Outer Code Box.js').

	self assert: 
'var y = Polyglot.import("y");
// CODE BEGIN:{"boxName":"Inner Code Box","exportVariables":["z"],"importVariables":["y"],"language":"js"}
const z = y * 3;
// CODE END
Polyglot.export("z", z);
' withUnixLineEndings equals: (result at: '/tmp/snippets/Inner Code Box.js')