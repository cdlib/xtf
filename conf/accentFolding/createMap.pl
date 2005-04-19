

# Loop through all the lines in the file...
while( <> ) {

    # Find entries that have decompositions. The last component in the regex
    # is a little tricky: it checks that there are some characters, but that
    # none of them are < or >, thus avoiding all compatability decompositions.
    #
    if( /^(\w+);[^;]*;[^;]*;[^;]*;[^;]*;([^;<>]+);/ ) {
        $src = $1;
        $dst = $2;

        # Break up all the components of the destination.
        @dstParts = split( /\s+/, $dst );
        $dstBase = "";
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
            }
        }

        # If the base character decomposes to a non-diacritical mark plus
        # one or more diacritical marks, then we've found a mapping we're
        # interested in.
        #
        if( $dstBase ne "" and $dstAccents ne "" ) {
            print "$src|$dstBase\n";
        }
    }
}
