dispatching
_dispatch: identifier arguments: arguments ifFail: failBlock
	| result |
	(identifier = 'new' and: [ self primitiveIsInstantiable ])
		ifTrue: [ ^ self primitiveInstantiate: arguments ].
	(self primitiveIsMemberReadable: identifier)
		ifFalse: [ ^ failBlock value ].
	(self primitiveIsMemberInvocable: identifier)
		ifTrue: [ ^ self primitiveInvoke: identifier arguments: arguments ].
	result := self primitiveReadMember: identifier.
	^ (result isTruffleObject and: [ result primitiveIsExecutable ])
		ifTrue: [ result primitiveExecuteWithArguments: arguments ]
		ifFalse: [ result ]