instance creation
on: aForeignObject
	(Interop hasArrayElements: aForeignObject)
		ifTrue: [ ^ self basicNew initialize on: aForeignObject ]
		ifFalse: [ self error: 'ForeignObject cannot be represented as collection' ]