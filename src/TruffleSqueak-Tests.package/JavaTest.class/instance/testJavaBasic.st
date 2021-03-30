testing
testJavaBasic
	self assert: Java primitiveIsHostLookupAllowed.

	self assert: (Interop isHostObject: (Java type: 'int[]')).
	self assert: (Interop isHostSymbol: (Java type: 'int[]')).
	self assert: (Interop isHostObject: ((Java type: 'int[]') new: 0)).
	self deny: (Interop isHostSymbol: ((Java type: 'int[]') new: 0)).

	self assert:  (Interop identityHashCode: (Java type: 'int[]')) > 0.
	self assert:  'int' equals: (Interop toDisplayString: (Java type: 'int')) asString.

	self assert: ((Java type: 'java.lang.System') getProperty: 'java.version') notEmpty.

	self assert: ((Java type: 'java.awt.Frame') allInstVarNames includes: 'NORMAL').
