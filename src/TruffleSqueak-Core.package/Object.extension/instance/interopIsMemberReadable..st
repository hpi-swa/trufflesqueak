*TruffleSqueak-Core-members
interopIsMemberReadable: member
	^ member isString and: [ (self class instVarIndexFor: member ifAbsent: 0) > 0 ]