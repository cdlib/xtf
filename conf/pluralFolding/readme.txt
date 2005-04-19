The default plural map is based on the "Part of Speech Database", one of the
databases compiled by Kevin Atkinson <kevina@users.sourceforge.net>, and
accessible (along with many other word lists) here:

    http://wordlist.sourceforge.net/
    
The createMap.pl script, in conjunction with the Lingua Perl module, scans
all the nouns in the word list and pluralizes them.

The final step is to compress the map using gzip, for more efficient storage
in CVS.