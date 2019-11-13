accessing
at: identifier put: value
	^ identifier isInteger
		ifTrue: [ Interop writeArrayElement: self index: identifier value: value ]
		ifFalse: [ Interop writeMember: self member: identifier value: value ]