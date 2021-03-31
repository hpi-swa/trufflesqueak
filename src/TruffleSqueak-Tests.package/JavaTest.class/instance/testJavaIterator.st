testing
testJavaIterator
	| list expectedValues iterator |
	list := (Java type: 'java.util.ArrayList') new.
	self assert: (Interop hasIterator: list).
	
	expectedValues := {true. 2.34. #(9 8 7 6 5). OrderedCollection new. Smalltalk}.
	expectedValues do: [ :ea | list add: ea ].
	
	iterator := Interop getIterator: list.
	self assert: (Interop isIterator: iterator).

	expectedValues do: [ :ea |
		self assert: (Interop hasIteratorNextElement: iterator).
		self assert: ea equals: (Interop getIteratorNextElement: iterator) ].
	self deny: (Interop hasIteratorNextElement: iterator).
