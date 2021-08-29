Extension { #name : #NumberParserError }

{ #category : #'*TruffleSqueak-Core-exception' }
NumberParserError >> interopGetExceptionType [
	^ Java parseExceptionType
]
