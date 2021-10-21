Class {
	#name : #AWFYScriptConsole,
	#superclass : #Object,
	#category : #'AWFY-Benchmarks'
}

{ #category : #printing }
AWFYScriptConsole class >> println: aMessage [
	FileStream stdout nextPutAll: aMessage; cr; flush
]
