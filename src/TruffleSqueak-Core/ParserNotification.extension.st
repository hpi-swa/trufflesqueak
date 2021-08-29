Extension { #name : #ParserNotification }

{ #category : #'*TruffleSqueak-Core-exception' }
ParserNotification >> interopGetExceptionType [
	^ Java parseExceptionType
]
