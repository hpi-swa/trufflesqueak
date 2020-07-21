binding
bindingOf: aString
	(bindings includesKey: aString) ifFalse: [
		aString first isUppercase
			ifTrue: [^nil]
			ifFalse: [bindings at: aString put: nil]
	].
	^ bindings associationAt: aString