*TruffleSqueak-Core-members
interopIsMemberReadable: member
	^ (self class instVarIndexFor: member ifAbsent: 0) > 0