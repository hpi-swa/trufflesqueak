evaluating
value
	^ (Interop isExecutable: self)
		ifTrue: [ Interop execute: self with: #() ]
		ifFalse: [ self ]