primitives
primitiveGetInternalLanguageInfo: aLanguageID
	"Accepts IDs for internal and public languages"
	<primitive: 'primitiveGetPublicLanguageInfo' module: 'PolyglotPlugin'>
	^ aLanguageID = LanguageInfoStub instance getId
		ifTrue: [ LanguageInfoStub instance ]
		ifFalse: [ nil ]