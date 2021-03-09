*TruffleSqueak-Core-members
interopReadMember: member
	^ self instVarAt: (self class
			instVarIndexFor: member asString
			ifAbsent: [
				(Interop memberToSelector: member for: self)
					ifNotNil: [ :sel |
						(self class lookupSelector: sel)
							ifNotNil: [ :cm | [ :args | self withArgs: args executeMethod: cm ] ]
							ifNil: [ nil "TODO: should never happen?" ] ]
					ifNil: [ nil "TODO" ] ] )