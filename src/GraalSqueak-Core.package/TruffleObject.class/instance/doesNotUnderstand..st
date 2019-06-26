error handling
doesNotUnderstand: aMessage
	| identifier arguments result |
	identifier := aMessage selector asString copyUpTo: $:.
	arguments := aMessage arguments.
	(identifier = 'new' and: [ self primitiveIsInstantiable ])
		ifTrue: [ ^ self primitiveInstantiate: arguments ].
	(self primitiveIsMemberReadable: identifier)
		ifFalse: [ ^ super doesNotUnderstand: aMessage ].
	(self primitiveIsMemberInvocable: identifier)
		ifTrue: [ ^ self primitiveInvoke: identifier arguments: arguments ].
	result := self primitiveReadMember: identifier.
	^ (result isTruffleObject and: [ result primitiveIsExecutable ])
		ifTrue: [ result primitiveExecuteWithArguments: arguments ]
		ifFalse: [ result ]