Extension { #name : #Stream }

{ #category : #'*TruffleSqueak-Core-iterator' }
Stream >> interopGetIteratorNextElement [
	^ self next
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Stream >> interopHasIteratorNextElement [
	^ self atEnd not
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Stream >> interopIsIterator [
	^ true
]
