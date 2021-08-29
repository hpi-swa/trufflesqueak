Class {
	#name : #PECodeBoxReadWriterTest,
	#superclass : #TestCase,
	#category : 'TruffleSqueak-Tools-Editor-Tests'
}

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> assertCodeBoxAt: anInteger in: aText [

	| attributes codeBox textAnchor |
	attributes := aText attributesAt: anInteger.
	self assert: 1 equals: attributes size.
	textAnchor := attributes first.
	self assert: (textAnchor isKindOf: TextAnchor).
	self assert: (textAnchor anchoredMorph model isKindOf: PECodeBox).
	codeBox := textAnchor anchoredMorph model.
	self assert: codeBox collapsed not.
	^ codeBox
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testAddCodeBoxesToSnippetFolder1 [

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
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testAddCodeBoxesToSnippetFolder2 [

	| result code attribute |
	
	"Test that adding code boxes works if there is no referenced code box and the snippet folder is nil"
	attribute := TextColor new.
	code := '123 abc 123 abc 123 abc' asText.
	code addAttribute: attribute from: 1 to: 10.
	result := PECodeBoxReadWriter addCodeBoxesTo: code file: nil.
	self assert: code equals: result first.
	self assert: result second isEmpty.
	"Verify that our text attribute is still there."
	self assert: attribute equals: (result first attributesAt: 1) first.
	
	code := '123 123 123'.
	result := PECodeBoxReadWriter addCodeBoxesTo: code file: nil.
	self assert: code asText equals: result first.
	self assert: result second isEmpty.
	
	"Test error message when the amount of begin and end markers for code boxes isn't the same"
	code := '
CODE BOX BEGIN: 

CODE BOX END

CODE BOX BEGIN: 
'.
	PEMockUIManager useDuring: [:uiManager | 
		result := PECodeBoxReadWriter addCodeBoxesTo: code file: nil.
		self assert: uiManager hasInformed.
		self assert: code asText equals: result first.
		self assert: result second isEmpty.].
	
	"Test error message when using multiple different line endings"
	code := '
CODE BOX BEGIN: 

CODE BOX END', String lf, 'def', String cr.
	PEMockUIManager useDuring: [:uiManager | 
		result := PECodeBoxReadWriter addCodeBoxesTo: code file: nil.
		self assert: uiManager hasInformed.
		self assert: code asText equals: result first.
		self assert: result second isEmpty.].
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testBaseNameOf [

	| rw |
	rw := PECodeBoxReadWriter.
	
	self assert: 'foo.txt' equals: (rw baseNameOf: '/tmp/foo.txt')
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testCodeBoxForSnippet_normal [

	| codeBoxFile codeBox innerCodeBoxes tmp |
	codeBoxFile := PEFileEntryTestFile new
		fullPath: '/tmp/codeBox.js';
		content: '
x = Polyglot.import("x")
# CODE BEGIN:{"boxName":"Code Box","importVariables":["x"],"language":"js","exportVariables":["y"]}
let y = 6;
y += x;
# CODE END
Polyglot.export("y", y)' withUnixLineEndings
		yourself.
	
	tmp := PECodeBoxReadWriter codeBoxForSnippet: codeBoxFile.
	codeBox := tmp first.
	innerCodeBoxes := tmp second.
	self
		assert: PECodeBox equals: codeBox class;
		assert: 'Code Box' equals: codeBox boxName;
		assert: {'x'} equals: codeBox importVariables;
		assert: {'y'} equals: codeBox exportVariables;
		assert: #js equals: codeBox language;
		assert: 'let y = 6;
y += x;' withUnixLineEndings equals: codeBox code asString;
		assert: codeBoxFile equals: codeBox fileEntry;
		assert: innerCodeBoxes isEmpty
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testCodeBoxForSnippet_recursive [

	| codeBoxFile1 codeBoxFile2 parentDirectory innerCodeBox codeBox innerCodeBoxes tmp |
	codeBoxFile1 := PEFileEntryTestFile new
		fullPath: '/tmp/codeBox1.js';
		content: '
x = Polyglot.import("x")
# CODE BEGIN:{"boxName":"Outer Code Box","importVariables":["x"],"language":"js","exportVariables":["z"]}
const y = 6 + x;
# CODE BOX BEGIN:./codeBox2.js
Polyglot.export("y", y);
Polyglot.evalFile("js", path="/tmp/codebox2.js");
const z = Polyglot.import("z");
# CODE BOX END
# CODE END
Polyglot.export("z", z)' withUnixLineEndings
		yourself.
	codeBoxFile2 := PEFileEntryTestFile new
		fullPath: '/tmp/codeBox2.js';
		content: '
y = Polyglot.import("y")
# CODE BEGIN:{"boxName":"Inner Code Box","importVariables":["y"],"language":"js","exportVariables":["z"]}
const z = y * 2;
# CODE END
Polyglot.export("z", z)' withUnixLineEndings
		yourself.
	parentDirectory := PEFileEntryTestDirectory new
		fullPath: '/tmp';
		children: {codeBoxFile1. codeBoxFile2};
		yourself.
	codeBoxFile1 parent: parentDirectory.
	codeBoxFile2 parent: parentDirectory.
	
	tmp := PECodeBoxReadWriter codeBoxForSnippet: codeBoxFile1.
	codeBox := tmp first.
	innerCodeBoxes := tmp second.
	
	self 
		assert: PECodeBox equals: codeBox class;
		assert: 'Outer Code Box' equals: codeBox boxName;
		assert: {'x'} equals: codeBox importVariables;
		assert: {'z'} equals: codeBox exportVariables;
		assert: #js equals: codeBox language;
		assert: ('const y = 6 + x;', String lf, (String value: 1)) withUnixLineEndings equals: codeBox code asString;
		assert: codeBoxFile1 equals: codeBox fileEntry.

	innerCodeBox := self assertCodeBoxAt: 18 in: codeBox code.
	
	self
		assert: 1 equals: innerCodeBoxes size;
		assert: innerCodeBox equals: innerCodeBoxes first;
		assert: 'Inner Code Box' equals: innerCodeBox boxName;
		assert: {'y'} equals: innerCodeBox importVariables;
		assert: {'z'} equals: innerCodeBox exportVariables;
		assert: #js equals: innerCodeBox language;
		assert: 'const z = y * 2;' equals: innerCodeBox code asString;
		assert: codeBoxFile2 fullPath equals: innerCodeBox fileEntry fullPath.
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testGetFilesToSaveFromFileIdLineEndings_noCodebox [

	| code codeFile result |
	codeFile := PEFileEntryTestFile new
		fullPath: '/tmp/code.js';
		yourself.
		
	code := '123
abc' withUnixLineEndings asText.


	result := PECodeBoxReadWriter getFilesToSaveFrom: code path: codeFile fullPath id: #js lineEndings: String lf.
	
	self assert: result size = 1.
	self assert: code asString equals: (result at: codeFile fullPath)
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testGetFilesToSaveFromFileIdLineEndings_oneExistingCodebox [

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
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testGetFilesToSaveFromFileIdLineEndings_oneNewCodebox [

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
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testGetFilesToSaveFromFileIdLineEndings_twoNewCodeboxes [

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
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testGetJsonForBox [

	| codeBox |
	codeBox := PECodeBox new
		boxName: 'name';
		exportVariables: {'a'. 'b'};
		importVariables: {'c'. 'd'};
		language: #js.
		
	self 
		assert: '{"boxName":"name","exportVariables":["a","b"],"importVariables":["c","d"],"language":"js"}' 
		equals: (PECodeBoxReadWriter getJsonForBox: codeBox)
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testMakeRelativeTo [

	| rw |
	rw := PECodeBoxReadWriter.
	
	self assert: './file.txt' equals: (rw make: '/tmp/file.txt' relativeTo: '/tmp').
	self assert: './../file.txt' equals: (rw make: '/file.txt' relativeTo: '/tmp').
	self assert: './../../file.txt' equals: (rw make: '/tmp/file.txt' relativeTo: '/tmp/foo/bar')
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> testParentFolderOf [

	| rw |
	rw := PECodeBoxReadWriter.
	
	self assert: '/tmp' equals: (rw parentFolderOf: '/tmp/foo.txt')
]

{ #category : #'as yet unclassified' }
PECodeBoxReadWriterTest >> withoutAbsolutePathsInSnippets: aBlock [

	| useAbsolutePathsForSnippets |
	useAbsolutePathsForSnippets := PolyglotEditor useAbsolutePathsForSnippets.
	PolyglotEditor useAbsolutePathsForSnippets: false.
	aBlock ensure: [PolyglotEditor useAbsolutePathsForSnippets: useAbsolutePathsForSnippets].
]
