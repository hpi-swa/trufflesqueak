interop
send: aMessage
	| identifier arguments |
	identifier := aMessage selector asString copyUpTo: $:.
	arguments := aMessage arguments.
	[ ^ self primitiveInvoke: identifier arguments: arguments ]
		on: Error do: [ :error |
			arguments size == 0 ifTrue: [ | result |
				result := self primitiveReadMember: identifier.
				^ result isExecutable
					ifTrue: [ self primitiveExecute: identifier arguments: arguments ]
					ifFalse: [ result ] ].
			error signal ]