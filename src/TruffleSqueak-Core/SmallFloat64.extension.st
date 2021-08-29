Extension { #name : #SmallFloat64 }

{ #category : #'*TruffleSqueak-Core-number' }
SmallFloat64 >> interopAsDouble [
	^ self
]

{ #category : #'*TruffleSqueak-Core-number' }
SmallFloat64 >> interopFitsInDouble [
	^ true
]
