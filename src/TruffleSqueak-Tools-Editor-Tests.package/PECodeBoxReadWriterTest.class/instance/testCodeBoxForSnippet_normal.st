as yet unclassified
testCodeBoxForSnippet_normal

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