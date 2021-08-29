Extension { #name : #Association }

{ #category : #'*TruffleSqueak-Core-arrays' }
Association >> interopGetArraySize [
	^ 2
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Association >> interopHasArrayElements [
	^ true
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Association >> interopIsArrayElementReadable: index [
	^ index = 0 or: [ index = 1 ]
]

{ #category : #'*TruffleSqueak-Core-arrays' }
Association >> interopReadArrayElement: index [
	^ index caseOf: { [ 0 ] -> [ key ]. [ 1 ] -> [ value ] }
		otherwise: [ self error: 'Unexpected index:', index asString ]
]
