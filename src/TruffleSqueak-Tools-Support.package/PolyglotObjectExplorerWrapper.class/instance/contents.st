accessing
contents
	^ ((Interop hasMembers: self object) ifTrue: [ self membersContents ] ifFalse: [{}]),
		((Interop hasArrayElements: self object) ifFalse: [{}] ifTrue: [
			((1 to: self object basicSize) collect: [:index |
				PolyglotObjectExplorerWrapper
					with: (self object basicAt: index)
					name: index printString
					model: self object ]) ])