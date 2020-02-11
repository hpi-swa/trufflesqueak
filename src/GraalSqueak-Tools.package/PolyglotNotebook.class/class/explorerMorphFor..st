helpers
explorerMorphFor: anObject
	| explorerView |
	explorerView := (ToolBuilder build: (PolyglotObjectExplorer explore: anObject))
		findA: PluggableTreeMorph.
	^ explorerView