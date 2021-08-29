Extension { #name : #String }

{ #category : #'*TruffleSqueak-Core-string' }
String >> interopAsString [
	^ Java asString: self
]
