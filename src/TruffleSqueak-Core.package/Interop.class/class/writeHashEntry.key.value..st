primitives hashes
writeHashEntry: anObject key: key value: value
	<primitive: 'primitiveWriteHashEntry' module: 'PolyglotPlugin'>
	^ (self isHashEntryWritable: anObject key: key)
		ifTrue: [ anObject interopWriteHashEntry: key and: value ]
		ifFalse: [ self error: 'Key ', key, ' is not writable' ]