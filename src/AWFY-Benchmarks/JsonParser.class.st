Class {
	#name : #JsonParser,
	#superclass : #Object,
	#instVars : [
		'input',
		'index',
		'line',
		'column',
		'current',
		'captureBuffer',
		'captureStart',
		'exceptionBlock'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
JsonParser class >> with: aJsonString [

    ^ self new initializeWith: aJsonString

]

{ #category : #'as yet unclassified' }
JsonParser >> endCapture [

    | captured |
    '' = captureBuffer
      ifTrue:  [ captured := input copyFrom: captureStart to: index - 1 ]
      ifFalse: [
        self pauseCapture.
        captured := captureBuffer.
        captureBuffer := '' ].
    captureStart := -1.
    
    ^ captured

]

{ #category : #'as yet unclassified' }
JsonParser >> error: message [

    exceptionBlock value: (ParseException with: message at: index
                                          line: line    column: column )

]

{ #category : #'as yet unclassified' }
JsonParser >> expected: expected [

    self isEndOfText ifTrue: [
      self error: 'Unexpected end of input, expected ', expected asString
    ].
    self error: 'Expected ', expected

]

{ #category : #'as yet unclassified' }
JsonParser >> initializeWith: string [

    input := string.
    index := 0.
    line  := 1.
    column := 0.
    current := nil.
    captureBuffer := ''.
    captureStart  := -1.

]

{ #category : #'as yet unclassified' }
JsonParser >> isDigit [

    current = '0' ifTrue: [^ true].
    current = '1' ifTrue: [^ true].
    current = '2' ifTrue: [^ true].
    current = '3' ifTrue: [^ true].
    current = '4' ifTrue: [^ true].
    current = '5' ifTrue: [^ true].
    current = '6' ifTrue: [^ true].
    current = '7' ifTrue: [^ true].
    current = '8' ifTrue: [^ true].
    current = '9' ifTrue: [^ true].
    ^ false

]

{ #category : #'as yet unclassified' }
JsonParser >> isEndOfText [

    ^ current isNil

]

{ #category : #'as yet unclassified' }
JsonParser >> isWhiteSpace [

    current = ' '  ifTrue: [^ true].
    current = '\t' ifTrue: [^ true].
    current = '\n' ifTrue: [^ true].
    current = '\r' ifTrue: [^ true].
    ^ false

]

{ #category : #'as yet unclassified' }
JsonParser >> parse [

    | result |
    exceptionBlock := [:ex | ^ ex ].
    self read.
    self skipWhiteSpace.
    result := self readValue.
    self skipWhiteSpace.
    self isEndOfText ifFalse: [ self error: 'Unexpected character'].
    ^ result

]

{ #category : #'as yet unclassified' }
JsonParser >> pauseCapture [

    captureBuffer := captureBuffer concatenate: (
      input copyFrom: captureStart to: index - 1).
    captureStart := -1

]

{ #category : #'as yet unclassified' }
JsonParser >> read [

    current = '\n' ifTrue: [
      line := line + 1.
      column := 0.
    ].
    
    index := index + 1.
    column := column + 1.

    input ifNil: [ self error:'input nil'].
    index <= input size
      ifTrue:  [ current := (input at: index) asString ]
      ifFalse: [ current := nil ]

]

{ #category : #'as yet unclassified' }
JsonParser >> readArray [

    | array |
    self read.
    array := JsonArray new.
    
    "Array might be empty"
    self skipWhiteSpace.
    (self readChar: ']') ifTrue: [
      ^ array
    ].
    
    self readArrayElement: array.
    [self readChar: ','] whileTrue: [
      self readArrayElement: array.
    ].

    (self readChar: ']') ifFalse: [
      self expected: '"," or "]"'
    ].
    ^ array

]

{ #category : #'as yet unclassified' }
JsonParser >> readArrayElement: array [

    self skipWhiteSpace.
    array add: self readValue.
    self skipWhiteSpace.

]

{ #category : #'as yet unclassified' }
JsonParser >> readChar: ch [

    current = ch ifFalse: [ ^ false ].
    self read.
    ^ true

]

{ #category : #'as yet unclassified' }
JsonParser >> readDigit [

    self isDigit ifFalse: [ ^ false ].
    self read.
    ^ true

]

{ #category : #'as yet unclassified' }
JsonParser >> readEscape [

    self read.    
    captureBuffer := captureBuffer concatenate: self readEscapeChar.
    self read

]

{ #category : #'as yet unclassified' }
JsonParser >> readEscapeChar [

    current = '"'  ifTrue: [ ^ '"'  ].
    current = '/'  ifTrue: [ ^ '/'  ].
    current = '\\' ifTrue: [ ^ '\\' ].

    current = 'b'  ifTrue: [ ^ '\b'  ].
    current = 'f'  ifTrue: [ ^ '\f'  ].
    current = 'n'  ifTrue: [ ^ '\n'  ].
    current = 'r'  ifTrue: [ ^ '\r'  ].
    current = 't'  ifTrue: [ ^ '\t'  ].

    "TODO: SOM doesn't have a way to create unicode characters."
    self expected: 'valid escape sequence. note, some are not supported'

]

{ #category : #'as yet unclassified' }
JsonParser >> readExponent [

    ((self readChar: 'e') not and: [
      (self readChar: 'E') not]) ifTrue: [ ^ false ].
    
    (self readChar: '+') ifFalse: [ self readChar: '-' ].
    
    self readDigit ifFalse: [ self expected: 'digit' ].

    [self readDigit] whileTrue: [].

    ^ true

]

{ #category : #'as yet unclassified' }
JsonParser >> readFalse [

    self read.
    self readRequiredChar: 'a'.
    self readRequiredChar: 'l'.
    self readRequiredChar: 's'.
    self readRequiredChar: 'e'.
    ^ JsonLiteral FALSE

]

{ #category : #'as yet unclassified' }
JsonParser >> readFraction [

    (self readChar: '.') ifFalse: [ ^ false ].
    
    self readDigit ifFalse: [ self expected: 'digit' ].
    
    [self readDigit] whileTrue: [].

    ^ true

]

{ #category : #'as yet unclassified' }
JsonParser >> readName [

    current = '"' ifFalse: [ self expected: 'name' ].
    ^ self readStringInternal

]

{ #category : #'as yet unclassified' }
JsonParser >> readNull [

    self read.
    self readRequiredChar: 'u'.
    self readRequiredChar: 'l'.
    self readRequiredChar: 'l'.
    ^ JsonLiteral NULL

]

{ #category : #'as yet unclassified' }
JsonParser >> readNumber [

    | firstDigit |
    self startCapture.
    self readChar: '-'.
    firstDigit := current.
    
    self readDigit ifFalse: [ self expected: 'digit' ].
    firstDigit ~= '0' ifTrue: [ [self readDigit] whileTrue: []].
        
    self readFraction.
    self readExponent.
    ^ JsonNumber new: self endCapture

]

{ #category : #'as yet unclassified' }
JsonParser >> readObject [

    | object |
    self read.
    object := JsonObject new.
    self skipWhiteSpace.

    (self readChar: '}') ifTrue: [
      ^ object
    ].
    
    self readObjectKeyValuePair: object.
    [self readChar: ','] whileTrue: [
      self readObjectKeyValuePair: object.
    ].
    
    (self readChar: '}') ifFalse: [
      self expected: '"," or "}"'
    ].
    
    ^ object

]

{ #category : #'as yet unclassified' }
JsonParser >> readObjectKeyValuePair: object [

    | name |
    self skipWhiteSpace.
    name := self readName.
    self skipWhiteSpace.
  
    (self readChar: ':') ifFalse: [ self expected: ':' ].
  
    self skipWhiteSpace.
  
    object add: name with: self readValue.
  
    self skipWhiteSpace.

]

{ #category : #'as yet unclassified' }
JsonParser >> readRequiredChar: ch [

    (self readChar: ch) ifFalse: [
      self expected: 'character: ', ch
    ]

]

{ #category : #'as yet unclassified' }
JsonParser >> readString [

    ^ JsonString new: self readStringInternal

]

{ #category : #'as yet unclassified' }
JsonParser >> readStringInternal [

    | string |
    self read.
    self startCapture.
    
    [current = '"'] whileFalse: [
      current = '\\' ifTrue: [
        self pauseCapture.
        self readEscape.
        self startCapture.
      ] ifFalse: [
        "if (current < 0x20) { throw expected('valid string character'); }"
        "we currently don't have a way to get the ordinal value for a character"
        "} else {"
        self read.
      ]
    ].
    string := self endCapture.
    self read.
    ^ string

]

{ #category : #'as yet unclassified' }
JsonParser >> readTrue [

    self read.
    self readRequiredChar: 'r'.
    self readRequiredChar: 'u'.
    self readRequiredChar: 'e'.
    ^ JsonLiteral TRUE

]

{ #category : #'as yet unclassified' }
JsonParser >> readValue [

    current = 'n' ifTrue: [ ^ self readNull   ].
    current = 't' ifTrue: [ ^ self readTrue   ].
    current = 'f' ifTrue: [ ^ self readFalse  ].
    current = '"' ifTrue: [ ^ self readString ].
    current = '[' ifTrue: [ ^ self readArray  ].
    current = '{' ifTrue: [ ^ self readObject ].
    
    "Is this really the best way to write this?, or better #or:?,
     but with all the nesting, it's just ugly."
    current = '-' ifTrue: [ ^ self readNumber ].
    current = '0' ifTrue: [ ^ self readNumber ].
    current = '1' ifTrue: [ ^ self readNumber ].
    current = '2' ifTrue: [ ^ self readNumber ].
    current = '3' ifTrue: [ ^ self readNumber ].
    current = '4' ifTrue: [ ^ self readNumber ].
    current = '5' ifTrue: [ ^ self readNumber ].
    current = '6' ifTrue: [ ^ self readNumber ].
    current = '7' ifTrue: [ ^ self readNumber ].
    current = '8' ifTrue: [ ^ self readNumber ].
    current = '9' ifTrue: [ ^ self readNumber ].
    
    "else"
    self expected: 'value'

]

{ #category : #'as yet unclassified' }
JsonParser >> skipWhiteSpace [

    [ self isWhiteSpace ]
      whileTrue:
        [ self read ].

]

{ #category : #'as yet unclassified' }
JsonParser >> startCapture [

    captureStart := index

]
