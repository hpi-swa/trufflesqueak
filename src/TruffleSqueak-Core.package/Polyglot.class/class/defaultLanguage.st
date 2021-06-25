default language
defaultLanguage
	^ defaultLanguage ifNil: [ defaultLanguage := self languageIdSmalltalk ]