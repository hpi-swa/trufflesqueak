Extension { #name : #SmallInteger }

{ #category : #'*TruffleSqueak-Core-number' }
SmallInteger >> interopAsDouble [
	^ self asFloat
]

{ #category : #'*TruffleSqueak-Core-number' }
SmallInteger >> interopAsLong [
	^ self
]

{ #category : #'*TruffleSqueak-Core-number' }
SmallInteger >> interopFitsInDouble [
	^ true
]

{ #category : #'*TruffleSqueak-Core-number' }
SmallInteger >> interopFitsInLong [
	^ true
]
