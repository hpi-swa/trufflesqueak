private
privateStyle: aText
	| languageId |
	languageId := view model languageInfo getId.
	RougeStyler default style: aText language: languageId
		ifFail: [ languageId = Polyglot languageIdSmalltalk
				ifTrue: [ super privateStyle: aText ] ]