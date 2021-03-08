*TruffleSqueak-Core-members
interopIsMemberInvocable: member
	^ (self class interopMemberToSelector: member) notNil