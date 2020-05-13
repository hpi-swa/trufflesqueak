as yet unclassified
testCodeForPolyglotExport

	self assert: 'Polyglot.export("name", value)' equals: (PELanguageInfoRuby codeForPolyglotExportOf: 'value' to: 'name')