error handling
doesNotUnderstand: aMessage
	| identifier arguments |
	identifier := aMessage selector asString copyUpTo: $:.
	arguments := aMessage arguments.
	^ self _dispatch: identifier arguments: arguments
		ifFail: [ ^ super doesNotUnderstand: aMessage ].