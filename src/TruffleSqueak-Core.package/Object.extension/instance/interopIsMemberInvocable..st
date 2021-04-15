*TruffleSqueak-Core-members
interopIsMemberInvocable: member
	^ (Interop memberToSelector: member for: self) notNil or: [
		(self class lookupSelector: #doesNotUnderstand:) ~~ (Object >> #doesNotUnderstand:) ]