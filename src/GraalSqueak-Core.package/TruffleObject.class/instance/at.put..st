accessing
at: index put: value
	^ self instVarNamed: (self class instVarNames at: index) put: value