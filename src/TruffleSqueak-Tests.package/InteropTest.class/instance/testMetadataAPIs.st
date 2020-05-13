testing
testMetadataAPIs
	(Polyglot isPolyglotEvalAllowed) ifFalse: [ ^self ].

	"All boxed objects have a language"
	self assert: (Interop hasLanguage: Smalltalk).
	self assert: (Interop getLanguage: Smalltalk) equals: (Interop getLanguage: Smalltalk).
	"Immediate values have no language"
	self deny: (Interop hasLanguage: 42).

	"All boxed objects provide their class as meta object"
	self assert: (Interop hasMetaObject: Smalltalk).
	self assert: Smalltalk class equals: (Interop getMetaObject: Smalltalk).
	self assert: Smalltalk class asString equals: (Interop getMetaQualifiedName: (Interop getMetaObject: Smalltalk)) asString.
	self assert: Smalltalk class asString equals: (Interop getMetaSimpleName: (Interop getMetaObject: Smalltalk)) asString.
	self assert: ((Interop toDisplayString: Smalltalk) asString includesSubstring: Smalltalk asString)