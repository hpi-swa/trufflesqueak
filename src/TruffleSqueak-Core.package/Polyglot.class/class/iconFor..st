icons
iconFor: aLanguageId
	"LanguageIDToIcon := nil"
	^ (LanguageIDToIcon ifNil: [
		LanguageIDToIcon := Dictionary newFromPairs: {
			self languageIdHost. self iconJava.
			self languageIdJava. self iconJava.
			self languageIdJS. self iconJS.
			self languageIdLLVM. self iconLLVM.
			self languageIdMarkdown. self iconMarkdown.
			self languageIdPython. self iconPython.
			self languageIdR. self iconR.
			self languageIdRuby. self iconRuby.
			self languageIdSmalltalk. self iconSmalltalk }
		]) at: aLanguageId ifAbsent: [ ToolIcons iconNamed: #breakpoint ]