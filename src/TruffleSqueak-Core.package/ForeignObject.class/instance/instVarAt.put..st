system primitives
instVarAt: index put: anObject
	^ self instVarNamed: (self allInstVarNames at: index) put: anObject