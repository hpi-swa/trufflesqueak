system primitives
instVarAt: index
	^ self instVarNamed: (self allInstVarNames at: index)