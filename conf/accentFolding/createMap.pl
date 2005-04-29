#!/usr/bin/perl

$fileName = shift;

# Make an initial pass and read all the names.
open( FILE, "<$fileName" ) or die( "Error opening $fileName" );
while( <FILE> ) {
    if( /^(\w+);([^;]*);/ ) {
        $code = $1;
        $name = $2;
        $codeToName{$code} = $name;
    }
}
close( FILE );

# Make a second pass and generate the actual mapping.
open( FILE, "<$fileName" ) or die( "Error opening $fileName" );
while( <FILE> ) {

    # Find entries that have decompositions. The last component in the regex
    # is a little tricky: it checks that there are some characters, but that
    # none of them are < or >, thus avoiding all compatability decompositions.
    #
    if( /^(\w+);([^;]*);[^;]*;[^;]*;[^;]*;([^;<>]+);/ ) {
        $src     = $1;
        $srcName = $2;
        $dst     = $3;

        # Break up all the components of the destination.
        @dstParts = split( /\s+/, $dst );
        $dstBase = "";
        $dstBaseNames = "";
        $dstAccents = "";
        for( $i = 0; $i <= $#dstParts; $i++ ) {

            # Look for characters in one of the blocks of combining
            # diacritical marks.
            #
            if( ($dstParts[$i] ge "0300" and $dstParts[$i] le "036F") or
                ($dstParts[$i] ge "1DC0" and $dstParts[$i] le "1DFF") or
                ($dstParts[$i] ge "20D0" and $dstParts[$i] le "20FF") )
            {
                $dstAccents .= $dstParts[$i] . " ";
            }
            else { 
                $dstBase .= $dstParts[$i] . " "; 
                $dstBaseNames .= $codeToName{$dstParts[$i]} . " ";
            }
        }

        # If the base character decomposes to a non-diacritical mark plus
        # one or more diacritical marks, then we've found a mapping we're
        # interested in.
        #
        if( $dstBase ne "" and $dstAccents ne "" ) {
            $srcName      = join( " ", map(ucfirst(lc($_)), 
                                  split(/\s/, $srcName)) );
            $dstBaseNames = join( " ", map(ucfirst(lc($_)),
                                  split(/\s/, $dstBaseNames)) );
            print "$src|$dstBase ; $srcName|$dstBaseNames\n";
        }
    }
}
close( FILE );
