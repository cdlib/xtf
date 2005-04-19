The default accent map is based on Unicode character database version 4.1.0,
available here:

    http://www.unicode.org/ucd/
    
The createMap.pl script, run in the UnicodeData.txt file, produces the accent
map. Essentially it looks for any code point that has a canoncial (not 
compatiblitiy) decomposition to another code point plus one or more 
diacritical marks in the following blocks: 0300-036F, 1DC0-1DFF, and 
20D0-20FF. It outputs a mapping from the original code point to the
non-diacritical code point.
