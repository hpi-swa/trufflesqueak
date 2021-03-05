primitives arrays
isArrayElementExisting: anObject index: anIndex
	<primitive: 'primitiveIsArrayElementExisting' module: 'PolyglotPlugin'>
	^ (self isArrayElementModifiable: anObject index: anIndex) or: [
		(self isArrayElementReadable: anObject index: anIndex) or: [
			(self isArrayElementRemovable: anObject index: anIndex) ] ]