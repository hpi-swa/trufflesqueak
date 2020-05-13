as yet unclassified
testGetFilesToSaveFromFileIdLineEndings_oneNewCodebox

	| code codeFile result codeBox rootDirectory |
	codeFile := PEFileEntryTestFile new
		fullPath: '/tmp/code.py';
		yourself.
	rootDirectory := PEFileEntryTestDirectory new
		fullPath: '/tmp';
		children: {codeFile};
		yourself.
	codeFile parent: rootDirectory.

	codeBox := PECodeBox new
		boxName: 'Code Box';
		importVariables: {'x'};
		exportVariables: {'y'};
		language: #js;
		code: 'var y = x + 1;'
		yourself.
		
	code := ('
x = 4',
String lf, (String value: 1), String lf,
'y') withUnixLineEndings asText.
	code addAttribute: (TextAnchor new
		anchoredMorph: (ToolBuilder build: codeBox);
		yourself) from: 8 to: 8.

	self withoutAbsolutePathsInSnippets: [result := PECodeBoxReadWriter
		getFilesToSaveFrom: code path: codeFile fullPath id: #python lineEndings: String lf].

	self assert: result size = 2.
	self assert: '
x = 4
# CODE BOX BEGIN:./snippets/Code Box.js
polyglot.export_value(x, "x")
polyglot.eval(language="js", path="./snippets/Code Box.js")
y = polyglot.import_value("y")
# CODE BOX END
y' withUnixLineEndings equals: (result at: '/tmp/code.py').
	
	self assert: 
'var x = Polyglot.import("x");
// CODE BEGIN:{"boxName":"Code Box","exportVariables":["y"],"importVariables":["x"],"language":"js"}
var y = x + 1;
// CODE END
Polyglot.export("y", y);
' withUnixLineEndings equals: (result at: '/tmp/snippets/Code Box.js')