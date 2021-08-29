Extension { #name : #Context }

{ #category : #'*TruffleSqueak-Core-scope' }
Context >> interopGetScopeParent [
	^ self sender
		ifNotNil: [ :s | s ]
		ifNil: [ Java throwUnsupportedMessageException ]
]

{ #category : #'*TruffleSqueak-Core-scope' }
Context >> interopHasScopeParent [
	^ self sender notNil
]

{ #category : #'*TruffleSqueak-Core-scope' }
Context >> interopIsScope [
	^ true
]
