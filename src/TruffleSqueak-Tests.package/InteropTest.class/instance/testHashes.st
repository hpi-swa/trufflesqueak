testing
testHashes
	| obj iterator |
	self deny: (Interop hasHashEntries: Smalltalk).
	
	obj := Dictionary new
		at: 'foo' put: 'bar';
		at: 13 put: 42;
		yourself.
	self assert: (Interop hasHashEntries: obj).
	self assert: obj size equals: (Interop getHashSize: obj).
	self assert: (Interop isHashEntryExisting: obj key: 'foo').
	self assert: (Interop isHashEntryExisting: obj key: 13).
	self deny: (Interop isHashEntryExisting: obj key: Smalltalk).
	self assert: (Interop isHashEntryInsertable: obj key: Smalltalk).
	self assert: (Interop isHashEntryModifiable: obj key: 'foo').
	self assert: (Interop isHashEntryReadable: obj key: 'foo').
	self assert: (Interop isHashEntryRemovable: obj key: 'foo').
	self assert: (Interop isHashEntryWritable: obj key: 'foo').
	self assert: (obj at: 'foo') equals: (Interop readHashValue: obj key: 'foo').
	self assert: (obj at: 'foo') equals: (Interop readHashValueOrDefault: obj key: 'foo' default: true).
	self assert: (Interop readHashValueOrDefault: obj key: 'bar' default: true).
	Interop removeHashEntry: obj key: 'foo'.
	self deny: (obj includesKey: 'foo').
	Interop writeHashEntry: obj key: Smalltalk value: Array.
	self assert: (obj includesKey: Smalltalk).
	self assert: Array equals: (Interop readHashValue: obj key: Smalltalk).

	"Iterate entries"
	iterator := Interop getHashEntriesIterator: obj.
	self assert: (Interop isIterator: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	self assert: (obj associationAt: 13) equals: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	self assert: (obj associationAt: Smalltalk) equals: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).

	"Iterate keys"
	iterator := Interop getHashKeysIterator: obj.
	self assert: (Interop isIterator: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	self assert: 13 equals: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	self assert: Smalltalk equals: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).

	"Iterate values"
	iterator := Interop getHashValuesIterator: obj.
	self assert: (Interop isIterator: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	self assert: (obj at: 13) equals: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	self assert: (obj at: Smalltalk) equals: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).
