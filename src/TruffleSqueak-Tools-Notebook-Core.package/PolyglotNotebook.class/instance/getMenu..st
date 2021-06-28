actions
getMenu: shiftKeyState 
	^ (shiftKeyState not or: [Preferences noviceMode])
		ifTrue: [TextEditor yellowButtonMenu]
		ifFalse: [TextEditor shiftedYellowButtonMenu]