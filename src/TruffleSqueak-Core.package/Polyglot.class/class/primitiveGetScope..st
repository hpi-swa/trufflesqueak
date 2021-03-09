primitives
primitiveGetScope: languageId
	<primitive: 'primitiveGetScope' module: 'PolyglotPlugin'>
	^ languageId = Polyglot languageIdSmalltalk
		ifTrue: [ SmalltalkInteropScope instance ]
		ifFalse: [ self primitiveFailed ]