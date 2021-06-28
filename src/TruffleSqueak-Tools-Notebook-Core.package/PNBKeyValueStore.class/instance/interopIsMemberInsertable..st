overrides
interopIsMemberInsertable: member
	^ member isString and: [ (self interopIsMemberReadable: member) not ]