testing
testJSMetadataAPIs
	| jsCode jsObject |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsCode := '{a: "Foo"}'.
	jsObject := (self jsEval: 'new Object(', jsCode, ')').

	self assert: (Interop hasLanguageId: jsObject).
	self assert: (Interop getLanguageId: jsObject) equals: (Interop getLanguageId: jsObject)
		description: 'Languages not equal'.
	self deny: (Interop isInternal: jsObject).

	"All boxed objects provide their class as meta object"
	self assert: (Interop hasMetaObject: jsObject).
	self assert: (Interop getMetaObject: jsObject) equals: (self jsEval: 'Object')
		description: 'Meta objects not equal'.
	self assert: 'Object' equals: (Interop getMetaQualifiedName: (Interop getMetaObject: jsObject)) asString.
	self assert: 'Object' equals: (Interop getMetaSimpleName: (Interop getMetaObject: jsObject)) asString.
	self assert: jsCode equals: (Interop toDisplayString: jsObject) asString.
	"GraalJS does not support meta parents yet"
	
	self assert: (jsObject isKindOf: (Interop getMetaObject: jsObject)).
	self assert: (jsObject respondsTo: #a).
	self deny: (jsObject respondsTo: #foo).
	