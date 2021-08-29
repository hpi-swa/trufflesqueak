Extension { #name : #Object }

{ #category : #'*TruffleSqueak-Tools-Core' }
Object >> polyglotExplore [
	PolyglotObjectExplorer openOn: self
]

{ #category : #'*TruffleSqueak-Tools-Core' }
Object >> polyglotInspect [
	PolyglotInspector openOn: self
]
