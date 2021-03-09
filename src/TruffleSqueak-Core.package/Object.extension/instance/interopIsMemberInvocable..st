*TruffleSqueak-Core-members
interopIsMemberInvocable: member
	^ (Interop memberToSelector: member for: self) notNil