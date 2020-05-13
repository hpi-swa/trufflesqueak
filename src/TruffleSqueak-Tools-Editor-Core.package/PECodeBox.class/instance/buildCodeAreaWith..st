building
buildCodeAreaWith: aBuilder

	| editArea |
	editArea := PEPluggableCodeSpec new
		model: self;
		autoHeight: true;
		setText: #code:;
		getText: #code;
		editText: #code:;
		setSelection: #privateSelection:;
		selection: #selection;
		styler: self textStyler;
		yourself.
	self editArea: editArea.
	^ editArea