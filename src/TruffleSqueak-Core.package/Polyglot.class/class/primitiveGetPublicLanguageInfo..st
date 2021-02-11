primitives
primitiveGetPublicLanguageInfo: aLanguageID
	<primitive: 'primitiveGetPublicLanguageInfo' module: 'PolyglotPlugin'>
	^ aLanguageID = LanguageInfoStub instance getId
		ifTrue: [ LanguageInfoStub instance ]
		ifFalse: [ nil ]