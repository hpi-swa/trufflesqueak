testing
testJavaHashes
	| map expectedValues iterator |
	map := (Java type: 'java.util.HashMap') new.
	self assert: (Interop hasHashEntries: map).
	self assert: 0 equals: (Interop getHashSize: map).

	self deny: (Interop isHashEntryExisting: map key: 42).
	self assert: Array equals: (Interop readHashValueOrDefault: map key: 42 default: Array).
	self assert: (Interop isHashEntryWritable: map key: 42).
	self assert: Smalltalk equals: (Interop writeHashEntry: map key: 42 value: Smalltalk).
	self assert: 1 equals: (Interop getHashSize: map).
	self assert: (Interop isHashEntryExisting: map key: 42).
	self assert: (Interop isHashEntryReadable: map key: 42).
	self assert: Smalltalk equals: (Interop readHashValue: map key: 42).
	self assert: (Interop isHashEntryModifiable: map key: 42).
	self assert: (Interop isHashEntryRemovable: map key: 42).
	self assert: map equals: (Interop removeHashEntry: map key: 42).
	self assert: 0 equals: (Interop getHashSize: map).
	
	expectedValues := {true. 2.34. #(9 8 7 6 5). OrderedCollection new. Smalltalk}.
	expectedValues withIndexDo: [ :v :i | Interop writeHashEntry: map key: i value: v ].
	self assert: expectedValues size equals: (Interop getHashSize: map).
	
	"Iterate entries"
	iterator := Interop getHashEntriesIterator: map.
	self assert: (Interop isIterator: iterator).
	expectedValues withIndexDo: [ :v :i | | entry |
		self assert: (Interop hasIteratorNextElement: iterator).
		entry := (Interop getIteratorNextElement: iterator).
		self assert: i equals: (entry at: 1).
		self assert: v equals: (entry at: 2) ].
	self deny: (Interop hasIteratorNextElement: iterator).
	
	"Iterate keys"
	iterator := Interop getHashKeysIterator: map.
	self assert: (Interop isIterator: iterator).
	expectedValues withIndexDo: [ :v :i |
		self assert: (Interop hasIteratorNextElement: iterator).
		self assert: i equals: (Interop getIteratorNextElement: iterator) ].
	self deny: (Interop hasIteratorNextElement: iterator).
	
	"Iterate values"
	iterator := Interop getHashValuesIterator: map.
	self assert: (Interop isIterator: iterator).
	expectedValues withIndexDo: [ :v :i |
		self assert: (Interop hasIteratorNextElement: iterator).
		self assert: v equals: (Interop getIteratorNextElement: iterator) ].
	self deny: (Interop hasIteratorNextElement: iterator).
