*TruffleSqueak-Core-members
interopReadMember: member
	| cm |
	cm := self class lookupSelector: (self class interopMemberToSelector: member).
	^ cm
		ifNil: [ nil ]
		ifNotNil: [ [ :args | self withArgs: args executeMethod: cm ] ]