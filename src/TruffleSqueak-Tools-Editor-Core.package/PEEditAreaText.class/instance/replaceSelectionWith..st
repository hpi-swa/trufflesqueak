public
replaceSelectionWith: aText
	"replaces the currently selected text by aText. If nothing is selected, then the text is inserted at the cursor"

	self unsavedCode: (self unsavedCode asText
		copyReplaceFrom: self selection start 
		to: self selection stop 
		with: aText).
	self changed: #unsavedCode.
	self selection: (self selection start to: (self selection stop + aText size))