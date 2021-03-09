testing
testBooleans
	self deny: (Interop isBoolean: Smalltalk).
	
	self assert: (Interop isBoolean: true).
	self assert: true == (Interop asBoolean: true).
	self assert: (Interop isBoolean: false).
	self assert: false == (Interop asBoolean: false).