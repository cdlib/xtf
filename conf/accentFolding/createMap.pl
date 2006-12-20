#!/usr/bin/perl

$unicodeDataFileName = shift;
$specialCharsFileName = shift;

# Make an initial pass and read all the names.
open( FILE, "<$unicodeDataFileName" ) or die( "Error opening $unicodeDataFileName" );
while( <FILE> ) {
    if( /^(\w+);([^;]*);/ ) {
        $code = $1;
        $name = $2;
        $codeToName{$code} = $name;
    }
}
close( FILE );

# Read in all the special mappings and record them in a hash
open( FILE, "<$specialCharsFileName" ) or die( "Error opening $specialCharsFileName" );
while( <FILE> ) {

    # Find lines that specify a mapping
    if( /^(\w\w\w\w)\|(.*)/ ) {
        $special{$1} = $2;
    }
}
close( FILE );


# Make a second pass and generate the actual mapping.
open( FILE, "<$unicodeDataFileName" ) or die( "Error opening $unicodeDataFileName" );
while( <FILE> ) {

    # Find entries that have decompositions.
    if( /^(\w+);([^;]*);[^;]*;[^;]*;[^;]*;(<[^>]+>)?\s?([^;]*);/ ) {
        $src     = $1;
        $srcName = $2;
        $type    = $3;
        $dst     = $4;

        # Skip codes that are above the 16-bit range Java can handle
        if( length($src) ne 4 ) { next; }

        # If we have a special mapping for this character, that overrides any 
        # other action.
        #
        if( exists $special{$src} ) {
            print "$src|$special{$src}\n";
            next;
        }

        # If the character is itself a diacritic, map it to nothing
        if( isDiacritic($src) ) {
            print "$src| ; $srcName|<remove>\n";
            next;
        }

        # If there's no decomposition, skip this character.
        if( $dst eq "" ) {
            next;
        }

        # Only allow certain non-canonical decompositions through, such
        # as those for superscript, subscript, circled, etc. These are
        # generally called "presentation forms".
        #
        if( $type ne "" &&!($type =~ /<(super|sub|circle|font|fraction|square|small|wide|narrow)>/) ) {
            next;
        }

        # Break up all the components of the destination.
        @dstParts = split( /\s+/, $dst );
        $dstBase = "";
        $dstBaseNames = "";
        $dstAccents = "";
        for( $i = 0; $i <= $#dstParts; $i++ ) {

            # Look for characters in one of the blocks of combining
            # diacritical marks.
            #
            if( isDiacritic($dstParts[$i]) ) {
                $dstAccents .= $dstParts[$i] . " ";
            }
            else { 
                $dstBase .= $dstParts[$i] . " "; 
                if( $dstBaseNames ne "" ) {
                    $dstBaseNames .= ", "
                }
                $dstBaseNames .= $codeToName{$dstParts[$i]};
            }
        }

        # If the base character decomposes to a non-diacritical mark plus
        # one or more diacritical marks, then we've found a mapping we're
        # interested in.
        #
        if( ($type ne "") || ($dstBase ne "" and $dstAccents ne "") ) {
            $srcName      = join( " ", map(ucfirst(lc($_)), 
                                  split(/\s/, $srcName)) );
            $dstBaseNames = join( " ", map(ucfirst(lc($_)),
                                  split(/\s/, $dstBaseNames)) );
            if( $type ne "" ) {
                print "$src|$dstBase ; $srcName|$type $dstBaseNames\n";
            }
            else {
                print "$src|$dstBase ; $srcName|$dstBaseNames\n";
            }
        }
    }
}
close( FILE );

sub isDiacritic {
    my $num = shift;

    return ($num ge "0300" and $num le "036F") or
           ($num ge "1DC0" and $num le "1DFF") or
           ($num ge "20D0" and $num le "20FF");
}
