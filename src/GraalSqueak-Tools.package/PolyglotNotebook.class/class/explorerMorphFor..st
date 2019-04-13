helpers
explorerMorphFor: anObject
	| explorerView |
	explorerView := (ToolBuilder build: (ObjectExplorer explore: anObject))
		findA: PluggableTreeMorph.
	^ explorerView