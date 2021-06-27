*TruffleSqueak-Core-members
interopIsMemberReadable: aMember
	^ self allMembers includes: (aMember asSymbol)