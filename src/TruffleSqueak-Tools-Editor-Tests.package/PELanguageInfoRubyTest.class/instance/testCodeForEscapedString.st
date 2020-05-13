as yet unclassified
testCodeForEscapedString

	self assert: '"foo\"bar"' equals: (PELanguageInfoRuby codeForEscapedString: 'foo"bar')