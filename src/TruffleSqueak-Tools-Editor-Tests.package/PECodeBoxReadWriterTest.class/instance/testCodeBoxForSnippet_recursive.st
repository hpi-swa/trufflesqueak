as yet unclassified
testCodeBoxForSnippet_recursive

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