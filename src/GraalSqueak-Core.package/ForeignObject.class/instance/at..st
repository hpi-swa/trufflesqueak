accessing
at: identifier
	^ identifier isInteger
		ifTrue: [ Interop readArrayElement: self index: identifier ]
		ifFalse: [ Interop readMember: self member: identifier ]