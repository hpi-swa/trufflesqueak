Class {
	#name : #PELanguageInfoMarkdown,
	#superclass : #PELanguageInfo,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #accessing }
PELanguageInfoMarkdown class >> color [

	^ Color fromString:  '#711C91'
]

{ #category : #accessing }
PELanguageInfoMarkdown class >> defaultLineEndings [

	^ String lf
]

{ #category : #accessing }
PELanguageInfoMarkdown class >> extensions [

	^ #(md)
]

{ #category : #accessing }
PELanguageInfoMarkdown class >> id [

	^ #markdown
]

{ #category : #accessing }
PELanguageInfoMarkdown class >> languageName [

	^ 'Markdown'
]

{ #category : #accessing }
PELanguageInfoMarkdown class >> rougeLexerName [

	^ 'markdown'
]
