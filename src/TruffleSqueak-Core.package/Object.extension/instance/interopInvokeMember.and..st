*TruffleSqueak-Core-members
interopInvokeMember: member and: arguments
	^ (self class interopMemberToSelector: member for: arguments)
		ifNotNil: [ :sel | self perform: sel withArguments: arguments]
		ifNil: [ self doesNotUnderstand: (
			Message selector: member asSymbol arguments: arguments) ]