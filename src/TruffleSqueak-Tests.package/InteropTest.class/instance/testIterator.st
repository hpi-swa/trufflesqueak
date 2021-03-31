testing
testIterator
	self deny: (Interop isIterator: Smalltalk).
	self deny: (Interop hasIterator: Smalltalk).
	self assert: (Interop hasIterator: Dictionary new).
	
	{#(42 Smalltalk true). #[12 15 42 92]. 'foo'.
		OrderedCollection with: 42 with: Smalltalk} do: [ :obj | | iter |
			iter := ReadStream on: obj.
			self assert: (Interop isIterator: iter).
			obj do: [ :ea |
				self assert: (Interop hasIteratorNextElement: iter).
				self assert: ea equals: (Interop getIteratorNextElement: iter) ].
			self deny: (Interop hasIteratorNextElement: iter) ].
