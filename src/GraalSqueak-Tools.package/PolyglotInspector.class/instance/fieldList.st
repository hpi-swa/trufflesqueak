overrides
fieldList
	self isForeign ifFalse: [^ super fieldList].
	^ #('self' 'all elements'), object allInstVarNames