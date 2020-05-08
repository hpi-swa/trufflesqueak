accessing
contents
	^ (Interop getMembers: self object) sorted withIndexCollect: [:member :index |
		PolyglotObjectExplorerWrapper
			with: (Interop readMember: self object member: member)
			name: member
			model: self object]