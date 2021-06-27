*TruffleSqueak-Core-members
interopReadMember: member
	^ Smalltalk at: member asSymbol ifAbsent: [ nil ]