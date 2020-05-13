public
openFile: aPCFileEntry

	editAreas detect: [:each | each model fileEntry = aPCFileEntry] 
		ifFound: [:existingEditArea | self makeEditAreaActive: existingEditArea]
		ifNone: [ | editAreaClass newEditArea |
			editAreaClass := PEEditAreaAbstract allSubclasses detectMax: [:each | 
				each supports: aPCFileEntry].
	
			newEditArea := ToolBuilder build: editAreaClass.
			newEditArea model openFile: aPCFileEntry.

			(tabs addTab: aPCFileEntry name)
				when: #clicked send: #makeEditAreaActive: to: self with: newEditArea; 
				when: #closed send: #removeEditArea: to: self with: newEditArea.
	
			self addEditArea: newEditArea.
			self makeEditAreaActive: newEditArea]