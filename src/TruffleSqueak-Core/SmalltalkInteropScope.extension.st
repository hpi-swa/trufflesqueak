Extension { #name : #SmalltalkInteropScope }

{ #category : #'*TruffleSqueak-Core-members' }
SmalltalkInteropScope >> interopGetMembers: includeInternal [
	^ self allMembers
]

{ #category : #'*TruffleSqueak-Core-members' }
SmalltalkInteropScope >> interopInvokeMember: member and: arguments [
	self shouldNotImplement
]

{ #category : #'*TruffleSqueak-Core-members' }
SmalltalkInteropScope >> interopIsMemberInvocable: aMember [
	^ false
]

{ #category : #'*TruffleSqueak-Core-members' }
SmalltalkInteropScope >> interopIsMemberReadable: aMember [
	^ self allMembers includes: (aMember asSymbol)
]

{ #category : #'*TruffleSqueak-Core-scope' }
SmalltalkInteropScope >> interopIsScope [
	^ true
]

{ #category : #'*TruffleSqueak-Core-members' }
SmalltalkInteropScope >> interopReadMember: member [
	^ Smalltalk at: member asSymbol ifAbsent: [ nil ]
]

{ #category : #'*TruffleSqueak-Core-string' }
SmalltalkInteropScope >> interopToDisplayString: allowSideEffects [
	^ 'Smalltalk'
]
