building
buildNameRowWith: aBuilder

	^ {PEPluggableLabelSpec new
			label: 'Name: ';
			yourself.
		aBuilder pluggableInputFieldSpec new
			model: self;
			setText: #boxName:;
			getText: #boxName;
			editText: #boxName:;
			yourself}