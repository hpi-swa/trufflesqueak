explorer
explorerContents
	^ (self explorableMembers asOrderedCollection collect: [:member |
		PolyglotObjectExplorerWrapper
			with: self
			member: member ]),
		((1 to: self basicSize) collect: [:index |
			PolyglotObjectExplorerWrapper
				with: (self basicAt: index)
				name: index printString
				model: self ])