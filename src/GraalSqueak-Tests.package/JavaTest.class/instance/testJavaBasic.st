testing
testJavaBasic
	self assert: Java primitiveIsHostLookupAllowed.

	self assert: (Interop isHostObject: (Java type: 'int[]')).
	self assert: (Interop isHostSymbol: (Java type: 'int[]')).
	self assert: (Interop isHostObject: ((Java type: 'int[]') new: 0)).
	self deny: (Interop isHostSymbol: ((Java type: 'int[]') new: 0)).

	self assert:  (Interop identityHash: (Java type: 'int[]')) > 0.
	self assert:  'JavaClass[int]' equals: (Interop stringRepresentation: (Java type: 'int')).

	self assert: ((Java type: 'java.lang.System') getProperty: 'java.version') notEmpty.