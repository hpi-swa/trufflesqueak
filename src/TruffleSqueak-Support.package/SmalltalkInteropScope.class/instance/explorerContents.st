overrides
explorerContents
	^ (self allMembers asOrderedCollection collect: [:member |
		PolyglotObjectExplorerWrapper
			with: self
			member: member ])