building
buildLanguageButtonWith: aBuilder

	^{aBuilder pluggableButtonSpec new
		model: self;
		label: #languageButtonLabel;
		action: #chooseLanguage;
		verticalResizing: #spaceFill;
		yourself}