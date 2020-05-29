testing
testJSMetadataAPIs
	| jsCode jsObject |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsCode := '{a: "Foo"}'.
	jsObject := (self jsEval: 'new Object(', jsCode, ')').

	self assert: (Interop hasLanguage: jsObject).
	self assert: (Interop getLanguage: jsObject) equals: (Interop getLanguage: jsObject)
		description: 'Languages not equal'.

	"All boxed objects provide their class as meta object"
	self assert: (Interop hasMetaObject: jsObject).
	self assert: (Interop getMetaObject: jsObject) equals: (Interop getMetaObject: jsObject)
		description: 'Meta objects not equal'.
	self assert: 'Object' equals: (Interop getMetaQualifiedName: (Interop getMetaObject: jsObject)) asString.
	self assert: 'Object' equals: (Interop getMetaSimpleName: (Interop getMetaObject: jsObject)) asString.
	self assert: jsCode equals: (Interop toDisplayString: jsObject) asString.
	