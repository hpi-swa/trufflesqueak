testing
testHashes
	| dict iterator iteratorData |
	self deny: (Interop hasHashEntries: Smalltalk).
	
	dict := Dictionary new
		at: 'foo' put: 'bar';
		at: 13 put: 42;
		yourself.
	self assert: (Interop hasHashEntries: dict).
	self assert: dict size equals: (Interop getHashSize: dict).
	self assert: (Interop isHashEntryExisting: dict key: 'foo').
	self assert: (Interop isHashEntryExisting: dict key: 13).
	self deny: (Interop isHashEntryExisting: dict key: Smalltalk).
	self assert: (Interop isHashEntryInsertable: dict key: Smalltalk).
	self assert: (Interop isHashEntryModifiable: dict key: 'foo').
	self assert: (Interop isHashEntryReadable: dict key: 'foo').
	self assert: (Interop isHashEntryRemovable: dict key: 'foo').
	self assert: (Interop isHashEntryWritable: dict key: 'foo').
	self assert: (dict at: 'foo') equals: (Interop readHashValue: dict key: 'foo').
	self assert: (dict at: 'foo') equals: (Interop readHashValueOrDefault: dict key: 'foo' default: true).
	self assert: (Interop readHashValueOrDefault: dict key: 'bar' default: true).
	Interop removeHashEntry: dict key: 'foo'.
	self deny: (dict includesKey: 'foo').
	Interop writeHashEntry: dict key: Smalltalk value: Array.
	self assert: (dict includesKey: Smalltalk).
	self assert: Array equals: (Interop readHashValue: dict key: Smalltalk).

	"Iterate entries"
	iterator := Interop getHashEntriesIterator: dict.
	self assert: (Interop isIterator: iterator).
	iteratorData := Dictionary new.
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).
	self assert: dict equals: iteratorData.

	"Iterate keys"
	iterator := Interop getHashKeysIterator: dict.
	self assert: (Interop isIterator: iterator).
	iteratorData := Set new.
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).
	self assert: dict keys asSet equals: iteratorData.

	"Iterate values"
	iterator := Interop getHashValuesIterator: dict.
	self assert: (Interop isIterator: iterator).
	iteratorData := Set new.
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self assert: (Interop hasIteratorNextElement: iterator).
	iteratorData add: (Interop getIteratorNextElement: iterator).
	self deny: (Interop hasIteratorNextElement: iterator).
	self assert: dict values asSet equals: iteratorData.
