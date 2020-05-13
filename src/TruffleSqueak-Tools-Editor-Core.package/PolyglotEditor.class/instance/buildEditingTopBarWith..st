building
buildEditingTopBarWith: aBuilder

	| buttons |
	buttons := {
		aBuilder pluggableActionButtonSpec new
			label: 'Split Pane';
			action: #splitPane;
			model: self;
			yourself.
		aBuilder pluggableActionButtonSpec new
			label: 'Run';
			action: #runSelected;
			model: self;
			yourself}.
	Smalltalk at: #CodeReuseBuilder ifPresent: [:codeReuse |
		buttons := buttons, {aBuilder pluggableActionButtonSpec new
			label: 'Search for code...';
			action: #openCodeReuseTool;
			help: 'Shortcut: CTRL-SHIFT-S';
			model: self;
			yourself}].
	buttons := buttons, {
		aBuilder pluggableActionButtonSpec new
			label: 'Export...';
			action: #polyglotExport;
			model: self;
			help: 'Shortcut: CTRL-SHIFT-X';
			yourself.
		aBuilder pluggableActionButtonSpec new
			label: 'Import...';
			action: #polyglotImport;
			help: 'Shortcut: CTRL-SHIFT-I';
			model: self;
			yourself.
		aBuilder pluggableActionButtonSpec new
			label: 'Evaluate...';
			action: #polyglotEvaluate;
			help: 'Shortcut: CTRL-SHIFT-E';
			model: self;
			yourself.
		aBuilder pluggableActionButtonSpec new
			label: 'Code Box';
			action: #polyglotCodeBox;
			help: 'Shortcut: CTRL-SHIFT-C';
			model: self;
			yourself}.
	
	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		children: buttons;
		frame: (LayoutFrame new leftFraction: 0 offset: 0;
					 topFraction: 0 offset: 0;
					 rightFraction: 1 offset: 0;
					 bottomFraction: 0 offset: 25)
		yourself