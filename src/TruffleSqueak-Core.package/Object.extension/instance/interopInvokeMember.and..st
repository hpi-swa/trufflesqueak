*TruffleSqueak-Core-members
interopInvokeMember: member and: arguments
	^ (Interop memberToSelector: member for: self arguments: arguments)
		ifNotNil: [ :sel | self perform: sel withArguments: arguments]
		ifNil: [ self doesNotUnderstand: (
			Message selector: member asSymbol arguments: arguments) ]