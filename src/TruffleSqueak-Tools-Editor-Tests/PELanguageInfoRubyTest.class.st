Class {
	#name : #PELanguageInfoRubyTest,
	#superclass : #TestCase,
	#category : 'TruffleSqueak-Tools-Editor-Tests'
}

{ #category : #'as yet unclassified' }
PELanguageInfoRubyTest >> testCodeForEscapedString [

	self assert: '"foo\"bar"' equals: (PELanguageInfoRuby codeForEscapedString: 'foo"bar')
]

{ #category : #'as yet unclassified' }
PELanguageInfoRubyTest >> testCodeForPolyglotExport [

	self assert: 'Polyglot.export("name", value)' equals: (PELanguageInfoRuby codeForPolyglotExportOf: 'value' to: 'name')
]

{ #category : #'as yet unclassified' }
PELanguageInfoRubyTest >> testCodeForPolyglotImport [

	self assert: 'Polyglot.import("foo")' equals: (PELanguageInfoRuby codeForPolyglotImport: 'foo')
]
