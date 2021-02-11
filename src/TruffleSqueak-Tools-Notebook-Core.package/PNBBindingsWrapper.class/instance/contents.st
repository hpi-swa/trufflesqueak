accessing
contents
	^ (Interop getMembers: self object) sorted collect: [:member |
		PolyglotObjectExplorerWrapper
			with: self object
			member: member]