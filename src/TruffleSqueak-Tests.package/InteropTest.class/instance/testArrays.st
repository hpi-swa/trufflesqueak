testing
testArrays
	(Polyglot isPolyglotEvalAllowed) ifFalse: [ ^self ].
	
	self deny: (Interop hasArrayElements: Smalltalk).
	
	{#(1 Smalltalk true nil).
		#[12 24 46 127].
		OrderedCollection with: 42 with: Smalltalk.
		WeakArray with: Smalltalk with: 42 with: Object new} do: [ :ea | 
			self assert: (Interop hasArrayElements: ea).
			self assert: ea size = (Interop getArraySize: ea).
			ea withIndexDo: [ :val :index |
				self assert: (Interop isArrayElementReadable: ea index: index).
				self assert: (Interop isArrayElementModifiable: ea index: index).
				self assert: val equals: (Interop readArrayElement: ea index: index) ].
			self assert: 7 equals: (Interop writeArrayElement: ea index: 1 value: 7).
			self assert: 7 equals: (Interop readArrayElement: ea index: 1) ].
	
	self assert: 3 equals: ((Java type: 'java.util.Arrays')
		binarySearch: #[0 8 255 1 192] key: ((Java type: 'java.lang.Byte') valueOf: '1')).
	self assert: 2 equals: ((Java type: 'java.util.Arrays')
		binarySearch: #[0 8 255 1 192] key: ((Java type: 'java.lang.Byte') valueOf: '-1'))
