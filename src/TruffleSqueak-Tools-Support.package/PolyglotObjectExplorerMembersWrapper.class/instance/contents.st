accessing
contents
	^ self members collect: [:member |
		PolyglotObjectExplorerWrapper
			with: self object
			member: member]