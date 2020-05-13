selection
setSelectedMorph: aMorph
	
	selectedWrapper := aMorph complexContents.
	realModel
		perform: setRealSelectedFileEntrySelector 
		with: (selectedWrapper ifNotNil: [:w | w item]).
	
	super setSelectedMorph: aMorph