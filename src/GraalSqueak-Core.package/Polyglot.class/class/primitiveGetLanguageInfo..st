primitives
primitiveGetLanguageInfo: aLanguageID
	<primitive: 'primitiveGetLanguageInfo' module: 'PolyglotPlugin'>
	^ aLanguageID = self defaultLanguage
		ifTrue: [ self defaultLanguageInfo ]
		ifFalse: [ nil ]