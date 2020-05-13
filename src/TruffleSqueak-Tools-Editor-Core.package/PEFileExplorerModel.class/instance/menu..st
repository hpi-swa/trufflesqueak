menus
menu: aMenuMorph

	self selectedEntry ifNil: [^ nil].
	self selectedEntry isFile ifFalse: [aMenuMorph
		add: 'New File' action: #menuCreateFile;
		add: 'New Directory' action: #menuCreateDirectory].
	
	^ aMenuMorph
		add: 'Rename' action: #menuRename;
		add: 'Delete' action: #menuDelete;
		add: 'Print contents to Transcript' action: #menuPrintToTranscript;
		yourself