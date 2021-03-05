*TruffleSqueak-Core-members
interopInvokeMember: member and: arguments
	^ self perform: (self class interopMemberToSelector: member) withArguments: arguments