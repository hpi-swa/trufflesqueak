primitives identity
isIdentical: anObject to: anotherObject
	<primitive: 'primitiveIsIdentical' module: 'PolyglotPlugin'>
	^ anObject interopIsIdentical: anotherObject and: nil