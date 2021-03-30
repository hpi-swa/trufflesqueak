dispatching
_dispatch: member arguments: arguments ifFail: failBlock
	(member = 'new' and: [ Interop isInstantiable: self ])
		ifTrue: [ ^ Interop instantiate: self with: arguments ].
	(Interop isMemberInvocable: self member: member)
		ifTrue: [ ^ Interop invokeMember: self member: member arguments: arguments ].
	(arguments size = 1 and: [ Interop isMemberWritable: self member: member])
		ifTrue: [^ Interop writeMember: self member: member value: arguments first ].
	^ (Interop isMemberReadable: self member: member)
		ifTrue: [ | result |
			result := Interop readMember: self member: member.
			(Interop isExecutable: result)
				ifTrue: [ Interop execute: result with: arguments ]
				ifFalse: [ result ]]
		ifFalse: failBlock