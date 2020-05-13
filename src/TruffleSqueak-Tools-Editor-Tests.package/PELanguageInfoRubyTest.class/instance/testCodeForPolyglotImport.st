as yet unclassified
testCodeForPolyglotImport

	self assert: 'Polyglot.import("foo")' equals: (PELanguageInfoRuby codeForPolyglotImport: 'foo')