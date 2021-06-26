private
privateStyle: aText
	| languageId |
	languageId := view model languageId.
	RougeStyler default style: aText language: languageId
		ifFail: [ languageId = Polyglot languageIdSmalltalk
				ifTrue: [ super privateStyle: aText ] ]