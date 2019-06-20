icons
iconFor: aLanguage
	"LanguageIDToIcon := nil"
	^ (LanguageIDToIcon ifNil: [
		LanguageIDToIcon := Dictionary newFromPairs: {
			self languageIdJava. self iconJava.
			self languageIdJS. self iconJS.
			self languageIdLLVM. self iconLLVM.
			self languageIdMarkdown. self iconMarkdown.
			self languageIdPython. self iconPython.
			self languageIdR. self iconR.
			self languageIdRuby. self iconRuby.
			self languageIdSqueakSmalltalk. self iconSqueakSmalltalk }
		]) at: aLanguage ifAbsent: [ nil ]