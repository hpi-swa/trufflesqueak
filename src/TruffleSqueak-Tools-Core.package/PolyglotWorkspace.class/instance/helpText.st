accessing
helpText
	^ 'Evaluate {1} expressions here. Switch the language in the window menu.'
		format: { self languageInfo getName }