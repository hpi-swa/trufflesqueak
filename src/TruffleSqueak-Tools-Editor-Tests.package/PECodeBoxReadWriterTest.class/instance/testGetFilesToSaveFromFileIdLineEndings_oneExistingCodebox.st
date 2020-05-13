as yet unclassified
testGetFilesToSaveFromFileIdLineEndings_oneExistingCodebox

	| code codeFile result codeBox rootDirectory codeBoxFile |
	
	codeFile := PEFileEntryTestFile new
		fullPath: '/tmp/code.py';
		yourself.
	codeBoxFile := PEFileEntryTestFile new
		fullPath: '/tmp/codeBox.js';
		yourself.
	rootDirectory := PEFileEntryTestDirectory new
		fullPath: '/tmp';
		children: {codeFile. codeBoxFile};
		yourself.
	codeFile parent: rootDirectory.
	codeBoxFile parent: rootDirectory.

	codeBox := PECodeBox new
		boxName: 'Code Box';
		importVariables: {'x'};
		exportVariables: {'y'};
		language: #js;
		code: 'const y = x + 4;';
		fileEntry: codeBoxFile
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
# CODE BOX BEGIN:./codeBox.js
polyglot.export_value(x, "x")
polyglot.eval(language="js", path="./codeBox.js")
y = polyglot.import_value("y")
# CODE BOX END
y' withUnixLineEndings equals: (result at: codeFile fullPath).

	self assert:
'var x = Polyglot.import("x");
// CODE BEGIN:{"boxName":"Code Box","exportVariables":["y"],"importVariables":["x"],"language":"js"}
const y = x + 4;
// CODE END
Polyglot.export("y", y);
' withUnixLineEndings  withUnixLineEndings equals: (result at: '/tmp/codeBox.js')