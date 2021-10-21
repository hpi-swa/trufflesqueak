Class {
	#name : #ScriptConsole,
	#superclass : #Object,
	#category : #'AWFY-Benchmarks'
}

{ #category : #printing }
ScriptConsole class >> println: aMessage [
	FileStream stdout nextPutAll: aMessage; cr; flush
]
