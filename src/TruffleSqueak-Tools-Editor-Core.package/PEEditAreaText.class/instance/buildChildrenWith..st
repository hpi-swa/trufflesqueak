building
buildChildrenWith: aBuilder

	^ {PEPluggableCodeSpec new
		model: self;
		setText: #codeFromEdit:;
		getText: #code;
		setEditText: #unsavedCode;
		editText: #unsavedCode:;
		setSelection: #privateSelection:;
		setLineEndings: #lineEndings;
		selection: #selection;
		onFocus: #triggerFocusEvent;
		onKeyStroke: #keyStroke:;
		styler: self textStyler;
		frame: (0@0 extent: 1@1);
		yourself}