primitives
primitiveGetLanguageInfo: aLanguageID
	<primitive: 'primitiveGetLanguageInfo' module: 'PolyglotPlugin'>
	^ aLanguageID = self defaultLanguageId
		ifTrue: [ self defaultLanguageInfo ]
		ifFalse: [ nil ]