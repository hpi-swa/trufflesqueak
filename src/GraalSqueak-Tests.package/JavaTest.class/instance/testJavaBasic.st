testing
testJavaBasic
	self assert: Java primitiveIsHostLookupAllowed.

	self assert: (Java type: 'int[]') primitiveIsHostObject.
	self assert: (Java type: 'int[]') primitiveIsHostSymbol.
	self assert: ((Java type: 'int[]') new: 0) primitiveIsHostObject.
	self deny: ((Java type: 'int[]') new: 0) primitiveIsHostSymbol.

	self assert:  (Java type: 'int[]') primitiveIdentityHash > 0.
	self assert:  'JavaClass[int]' equals: (Java type: 'int') primitiveStringRepresentation.

	self assert: ((Java type: 'java.lang.System') getProperty: 'java.version') notEmpty.