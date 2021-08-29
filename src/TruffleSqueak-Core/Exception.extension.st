Extension { #name : #Exception }

{ #category : #'*TruffleSqueak-Core-exception' }
Exception >> interopGetExceptionMessage [
	^ messageText
]

{ #category : #'*TruffleSqueak-Core-exception' }
Exception >> interopGetExceptionType [
	^ Java runtimeExceptionType
]

{ #category : #'*TruffleSqueak-Core-exception' }
Exception >> interopHasExceptionMessage [
	^ messageText ~~ nil
]

{ #category : #'*TruffleSqueak-Core-exception' }
Exception >> interopIsException [
	^ true
]

{ #category : #'*TruffleSqueak-Core-exception' }
Exception >> interopThrowException [
	Java throwAsPolyglotException: self
]
