*TruffleSqueak-Core-scope
interopGetScopeParent
	^ self sender
		ifNotNil: [ :s | s ]
		ifNil: [ Java throwUnsupportedMessageException ]