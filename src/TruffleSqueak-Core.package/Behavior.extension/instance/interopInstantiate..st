*TruffleSqueak-Core-instantiable
interopInstantiate: arguments
	^ arguments isEmpty
		ifTrue: [ self new ]
		ifFalse: [ | arg |
			arg := arguments first.
			(arg isInteger and: [ arg isLarge not ])
				ifTrue: [ self new: arg ]
				ifFalse: [ self new ]]