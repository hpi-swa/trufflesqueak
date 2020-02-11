accessing
membersWrapper
	^ PolyglotObjectExplorerMembersWrapper
			with: self object
			name: (Interop getMemberSize: self object) asString
			model: self
			parent: self