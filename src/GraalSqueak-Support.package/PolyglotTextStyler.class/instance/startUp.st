as yet unclassified
startUp
	((Polyglot isPermitted: 'ruby') and:
		[ (Smalltalk os platformName = 'Win32') not ]) ifTrue: [
			"Preload rouge:"
			[ Polyglot eval: 'ruby' string: 'require ''rouge''' ] on: Error do: [] ]