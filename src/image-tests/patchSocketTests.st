| socketClasses matchingRefs replacePatternsIn changedMethods |

socketClasses := Smalltalk allClasses select: [ :cls |
    (cls inheritsFrom: TestCase) and: [
        cls name includesSubstring: 'Socket' caseSensitive: false ] ].

matchingRefs := OrderedCollection new.

socketClasses do: [ :cls |
    cls selectorsAndMethodsDo: [ :selector :method |
        ((method sendsSelector: #forMilliseconds:) or: [ method sendsSelector: #wait ])
            ifTrue: [
                (method literals anySatisfy: [ :lit | lit isInteger and: [ lit < 100 ] ])
                    ifTrue: [ matchingRefs add: (MethodReference class: cls selector: selector) ] ] ] ].

"Uncomment to browse changed methods."
"SystemNavigation default
    browseMessageList: matchingRefs
    name: 'Socket Tests with Short Delays'."

"Helper block to scan and replace patterns without regex"
replacePatternsIn := [ :source :prefix :isPostfix |
    | currentSource idx |
    currentSource := source.
    idx := 1.
    [ (idx := currentSource indexOfSubCollection: prefix startingAt: idx) > 0 ] whileTrue: [
        | start end numVal numStr |
        isPostfix ifTrue: [
            end := idx - 1.
            [ end >= 1 and: [ (currentSource at: end) isSeparator ] ] whileTrue: [ end := end - 1 ].
            start := end.
            [ start >= 1 and: [ (currentSource at: start) isDigit ] ] whileTrue: [ start := start - 1 ].
            start := start + 1.
            (start <= end) ifTrue: [
                numStr := currentSource copyFrom: start to: end.
                numVal := numStr asInteger.
                (numVal notNil and: [ numVal < 100 ]) ifTrue: [
                    currentSource := (currentSource copyFrom: 1 to: start - 1) , '100' , (currentSource copyFrom: end + 1 to: currentSource size).
                    idx := idx + (100 asString size - numStr size).
                ].
            ].
            idx := idx + prefix size.
        ] ifFalse: [
            start := idx + prefix size.
            [ start <= currentSource size and: [ (currentSource at: start) isSeparator ] ] whileTrue: [ start := start + 1 ].
            end := start.
            [ end <= currentSource size and: [ (currentSource at: end) isDigit ] ] whileTrue: [ end := end + 1 ].
            (start < end) ifTrue: [
                numStr := currentSource copyFrom: start to: end - 1.
                numVal := numStr asInteger.
                (numVal notNil and: [ numVal < 100 ]) ifTrue: [
                    currentSource := (currentSource copyFrom: 1 to: start - 1) , '100' , (currentSource copyFrom: end to: currentSource size).
                    idx := start + 3.
                ] ifFalse: [ idx := end ].
            ] ifFalse: [ idx := idx + prefix size ].
        ].
    ].
    currentSource.
].

changedMethods := OrderedCollection new.

matchingRefs do: [ :ref |
    | class selector oldSource newSource category |
    class := ref actualClass.
    selector := ref selector.
    oldSource := (class sourceCodeAt: selector) asString.
    newSource := oldSource.

    "1. Replace 'Delay forMilliseconds: <digits>'"
    newSource := replacePatternsIn value: newSource value: 'forMilliseconds:' value: false.

    "2. Replace '<digits> milliSeconds' and '<digits> milliseconds'"
    newSource := replacePatternsIn value: newSource value: 'milliSeconds' value: true.
    newSource := replacePatternsIn value: newSource value: 'milliseconds' value: true.

    newSource ~= oldSource ifTrue: [
        category := class whichCategoryIncludesSelector: selector.
        class compile: newSource classified: category.
        changedMethods add: (class name , '>>#' , selector).
    ].
].

FileStream stdout nextPutAll: ('Successfully rewrote and recompiled {1} Socket testing methods to 100 ms min Delays.' format: { changedMethods size }).
changedMethods do: [ :methodName |
    FileStream stdout crtab; nextPutAll: methodName.
].
FileStream stdout cr; flush.
