class membership
respondsTo: aSymbol 
	^ (super respondsTo: aSymbol) or: [
		(Interop isMemberReadable: self member: aSymbol) or: [
			Interop isMemberInvocable: self member: aSymbol ] ]