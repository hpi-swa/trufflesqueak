panes
addPane

	self panes add: (PETabbedPane new yourself).
	self panes last
		when: #activeEditAreaContentChanged evaluate: [:editArea |
			self editAreaUpdated: editArea model];
		when: #focused evaluate: [:pane | 
			self panes do: [:each | each setAllTabsToNonActive] without: pane];
		when: #closed evaluate: [:pane |
			self panes remove: pane.
			self panesChanged];
		when: #activeEditAreaGeneratePolyglotCodeBox send: #polyglotCodeBox to: self;
		when: #activeEditAreaGeneratePolyglotEvaluate send: #polyglotEvaluate to: self;
		when: #activeEditAreaGeneratePolyglotImport send: #polyglotImport to: self;
		when: #activeEditAreaGeneratePolyglotExport send: #polyglotExport to: self;
		when: #activeEditAreaSearchForCode send: #openCodeReuseTool to: self;
		when: #newSnippetDirectory send: #addSnippetDirectory: to: self.
	
	self panesChanged