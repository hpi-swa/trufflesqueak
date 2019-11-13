dispatching
_dispatch: member arguments: arguments ifFail: failBlock
	| result |
	(member = 'new' and: [ Interop isInstantiable: self ])
		ifTrue: [ ^ Interop instantiate: self with: arguments ].
	(Interop isMemberReadable: self member: member)
		ifFalse: [ ^ failBlock value ].
	(Interop isMemberInvocable: self member: member)
		ifTrue: [ ^ Interop invokeMember: self member: member arguments: arguments ].
	result := Interop readMember: self member: member.
	^ (result isTruffleObject and: [ Interop isExecutable: result ])
		ifTrue: [ Interop execute: result with: arguments ]
		ifFalse: [ result ]