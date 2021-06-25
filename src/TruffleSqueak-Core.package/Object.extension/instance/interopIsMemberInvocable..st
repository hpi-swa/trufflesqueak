*TruffleSqueak-Core-members
interopIsMemberInvocable: member
	^  member isString and: [
			(Interop memberToSelector: member for: self) notNil or: [
				(self class lookupSelector: #doesNotUnderstand:) ~~ (Object >> #doesNotUnderstand:) ] ]