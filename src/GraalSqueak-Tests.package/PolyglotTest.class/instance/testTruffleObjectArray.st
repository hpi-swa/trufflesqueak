testing
testTruffleObjectArray
	| jsArray |
	jsArray := (Polyglot eval: 'js' string: '[1, 3, 4]').

	self assert: jsArray primitiveHasArrayElements.
	self assert: 3 equals: jsArray primitiveGetArraySize.
	self assert: (jsArray primitiveIsArrayElementExisting: 2).
	self deny: (jsArray primitiveIsArrayElementExisting: 5).
	self deny: (jsArray primitiveIsArrayElementInsertable: 2).
	self assert: (jsArray primitiveIsArrayElementInsertable: 5).
	self assert: (jsArray primitiveIsArrayElementModifiable: 2).
	self deny: (jsArray primitiveIsArrayElementModifiable: 5).
	self assert: (jsArray primitiveIsArrayElementReadable: 2).
	self deny: (jsArray primitiveIsArrayElementReadable: 5).
	self assert: (jsArray primitiveIsArrayElementRemovable: 2).
	self deny: (jsArray primitiveIsArrayElementRemovable: 5).
	self assert: (jsArray primitiveIsArrayElementWritable: 2).
	self assert: (jsArray primitiveIsArrayElementWritable: 5).

	self assert: 3 equals: (jsArray primitiveReadArrayElement: 2).
	self assert: 5 equals: (jsArray primitiveWriteArrayElement: 2 value: 5).
	self assert: 5 equals: (jsArray primitiveReadArrayElement: 2).
	"TODO: #primitiveRemoveArrayElement: not working correctly in Graal.js (s)ee https://github.com/graalvm/graaljs/issues/218)."
	"self assert: jsArray equals: (jsArray primitiveRemoveArrayElement: 2)."
