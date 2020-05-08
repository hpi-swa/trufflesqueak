testing
testJSMetadataAPIs
	| jsCode jsObject |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsCode := '{a: "Foo"}'.
	jsObject := (Polyglot eval: Polyglot languageIdJS string: 'new Object(', jsCode, ')').

	self assert: (Interop hasLanguage: jsObject).
	self assert: (Interop getLanguage: jsObject) equals: (Interop getLanguage: jsObject).

	"All boxed objects provide their class as meta object"
	self assert: (Interop hasMetaObject: jsObject).
	self assert: (Interop getMetaObject: jsObject) equals: (Interop getMetaObject: jsObject).
	self assert: 'Object' equals: (Interop getMetaQualifiedName: (Interop getMetaObject: jsObject)) asString.
	self assert: 'Object' equals: (Interop getMetaSimpleName: (Interop getMetaObject: jsObject)) asString.
	self assert: jsCode equals: (Interop toDisplayString: jsObject) asString.
	