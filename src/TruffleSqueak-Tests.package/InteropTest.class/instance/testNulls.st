testing
testNulls
	self deny: (Interop isNull: Smalltalk).
	self deny: (Interop isNull: UndefinedObject).
	self assert: (Interop isNull: nil)
