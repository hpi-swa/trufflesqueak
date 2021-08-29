Class {
	#name : #PolyglotObjectExplorerWrapper,
	#superclass : #ObjectExplorerWrapper,
	#category : 'TruffleSqueak-Tools-Support'
}

{ #category : #'instance creation' }
PolyglotObjectExplorerWrapper class >> with: anObject member: aMember [
	^ self
			with: ((Interop isMemberReadable: anObject member: aMember)
				ifTrue: [ Interop readMember: anObject member: aMember ]
				ifFalse: [ nil ])
			name: aMember
			model: anObject
]

{ #category : #accessing }
PolyglotObjectExplorerWrapper >> contents [
	^ ((Interop hasMembers: self object) ifTrue: [ self membersContents ] ifFalse: [{}]),
		((Interop hasArrayElements: self object) ifFalse: [{}] ifTrue: [
			((1 to: self object basicSize) collect: [:index |
				PolyglotObjectExplorerWrapper
					with: (self object basicAt: index)
					name: index printString
					model: self object ]) ])
]

{ #category : #accessing }
PolyglotObjectExplorerWrapper >> icon [
	^ (Interop hasLanguage: self object)
		ifTrue: [ Polyglot iconFor: (Interop getLanguageInfo: self object) getId asString ]
		ifFalse: [ ToolIcons iconNamed: #blank ]
]

{ #category : #accessing }
PolyglotObjectExplorerWrapper >> membersContents [
	| invocables readables remainder |
	invocables := OrderedCollection new.
	readables := OrderedCollection new.
	remainder := OrderedCollection new.
	(Interop getMembers: self object) sorted do: [ :ea |
		(Interop isMemberInvocable: self object member: ea)
			ifTrue: [ invocables add: ea ]
			ifFalse: [ (Interop isMemberReadable: self object member: ea)
				ifTrue: [ readables add: ea ]
				ifFalse: [ remainder add: ea ] ] ].
	^ (remainder ifNotEmpty: [ { PolyglotObjectExplorerMembersWrapper
			with: self object
			name: 'Non-readable members'
			model: self
			parent: self
			members: remainder } ] ifEmpty: [ {} ]),
		(readables ifNotEmpty: [ { PolyglotObjectExplorerMembersWrapper
			with: self object
			name: 'Readable members'
			model: self
			parent: self
			members: readables } ] ifEmpty: [ {} ]),
		(invocables ifNotEmpty: [ { PolyglotObjectExplorerMembersWrapper
			with: self object
			name: 'Invocable members'
			model: self
			parent: self
			members: invocables } ] ifEmpty: [ {} ])
]
