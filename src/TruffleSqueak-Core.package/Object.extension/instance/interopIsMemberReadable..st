*TruffleSqueak-Core-members
interopIsMemberReadable: member
	^ self respondsTo: (self class interopMemberToSelector: member)