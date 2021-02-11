accessing
membersContents
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