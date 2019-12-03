primitives
removeArrayElement: anObject index: anIndex
	<primitive: 'primitiveRemoveArrayElement' module: 'PolyglotPlugin'>
	(self isArrayElementRemovable: anObject index: anIndex)
		ifFalse: [ self error: 'Array element is not removable at index #', anIndex ]
		ifTrue: [ self primitiveFailed ]