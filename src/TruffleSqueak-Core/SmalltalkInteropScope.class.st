Class {
	#name : #SmalltalkInteropScope,
	#superclass : #Object,
	#classVars : [
		'Instance'
	],
	#category : #'TruffleSqueak-Core'
}

{ #category : #accessing }
SmalltalkInteropScope class >> instance [
	"Answer the receiver's instance"
	^ Instance ifNil: [ Instance := self new ]
]

{ #category : #accessing }
SmalltalkInteropScope >> allMembers [
	^ Smalltalk globals keys
]

{ #category : #overrides }
SmalltalkInteropScope >> explorerContents [
	^ (self allMembers asOrderedCollection collect: [:member |
		PolyglotObjectExplorerWrapper
			with: self
			member: member ])
]

{ #category : #overrides }
SmalltalkInteropScope >> hasContentsInExplorer [
	^ true
]

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
