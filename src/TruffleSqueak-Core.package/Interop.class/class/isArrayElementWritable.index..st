primitives arrays
isArrayElementWritable: anObject index: anIndex
	<primitive: 'primitiveIsArrayElementWritable' module: 'PolyglotPlugin'>
	^ (self isArrayElementModifiable: anObject index: anIndex) or: [
		(self isArrayElementInsertable: anObject index: anIndex) ]