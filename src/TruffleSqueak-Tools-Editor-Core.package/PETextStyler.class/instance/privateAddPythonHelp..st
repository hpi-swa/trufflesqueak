private
privateAddPythonHelp: aText
	
	| styledText textString offset |
	self languageInfo ifNil: [^ aText].
	self languageInfo isPython ifFalse: [^ aText].

	self removePreviousAnnotationMorphs: aText.

	textString := aText asString.
	styledText := aText.
	offset := 0.
	textString doWithIndex: [:each :index |
		each = $;
			ifTrue: [
				styledText := styledText
					replaceFrom: index + offset
					to: index +offset
					with: (Text withAll: ';') , self createSemicolonAnnotationMorph.
				offset := offset + 1]].
		
	textString := styledText asString.
	offset := 0.
	textString doWithIndex: [:each :index |
		each = ${
			ifTrue: [
				styledText := styledText
					replaceFrom: index + offset
					to: index +offset
					with: (Text withAll: '{') , self createWrongForAnnotationMorph.
				offset := offset + 1]].
	
	^ styledText