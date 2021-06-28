overrides
interopIsMemberReadable: member
	^ member isString and: [ self dictionary includesKey: member asString ]