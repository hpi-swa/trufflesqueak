*TruffleSqueak-Core-members
interopIsMemberReadable: aMember
	^ Smalltalk globals keys includes: (aMember asSymbol)