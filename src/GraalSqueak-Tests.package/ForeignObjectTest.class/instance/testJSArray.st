testing
testJSArray
	| jsArray stCollection |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsArray := (Polyglot eval: Polyglot languageIdJS string: '[1, 3, 4]').

	self assert: (Interop hasArrayElements: jsArray).
	self assert: 3 equals: (Interop getArraySize: jsArray).
	self assert: (Interop isArrayElementExisting: jsArray index: 2).
	self deny: (Interop isArrayElementExisting: jsArray index: 5).
	self deny: (Interop isArrayElementInsertable: jsArray index: 2).
	self assert: (Interop isArrayElementInsertable: jsArray index: 5).
	self assert: (Interop isArrayElementModifiable: jsArray index: 2).
	self deny: (Interop isArrayElementModifiable: jsArray index: 5).
	self assert: (Interop isArrayElementReadable: jsArray index: 2).
	self deny: (Interop isArrayElementReadable: jsArray index: 5).
	self assert: (Interop isArrayElementRemovable: jsArray index: 2).
	self deny: (Interop isArrayElementRemovable: jsArray index: 5).
	self assert: (Interop isArrayElementWritable: jsArray index: 2).
	self assert: (Interop isArrayElementWritable: jsArray index: 5).

	self assert: 3 equals: (Interop readArrayElement: jsArray index: 2).
	self assert: 5 equals: (Interop writeArrayElement: jsArray index: 2 value: 5).
	self assert: 5 equals: (Interop readArrayElement: jsArray index: 2).
	"TODO: #removeArrayElement: not working correctly in Graal.js (s)ee https://github.com/graalvm/graaljs/issues/218)."
	"self assert: jsArray equals: (Interop removeArrayElement: jsArray index: 2)."
	
	jsArray := (Polyglot eval: Polyglot languageIdJS string: '[1, 2, 3, 4]').
	self assert: 4 equals: jsArray size.
	1 to: jsArray size do: [ :i |
		self assert: i equals: (jsArray at: i) ].
	
	stCollection := jsArray asCollection.
	self assert: (stCollection includes: 1).
	self assert: 10 equals: stCollection sum.
	self assert: 4 equals: stCollection max.
	self assert: #(1 4 9 16) equals: (stCollection asArray collect: [ :e | e * e ])
