Extension { #name : #BlockClosure }

{ #category : #'*TruffleSqueak-Core-executable' }
BlockClosure >> interopExecute: arguments [
	^ (self numArgs == arguments size)
		ifTrue: [ self valueWithArguments: arguments ]
		ifFalse: [ "TODO: throw ArityException" nil ]
]

{ #category : #'*TruffleSqueak-Core-executable' }
BlockClosure >> interopGetExecutableName [
	^ self asString
]

{ #category : #'*TruffleSqueak-Core-executable' }
BlockClosure >> interopHasExecutableName [
	^ true
]

{ #category : #'*TruffleSqueak-Core-executable' }
BlockClosure >> interopIsExecutable [
	^ true
]
