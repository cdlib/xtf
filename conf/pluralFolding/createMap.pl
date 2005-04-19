
use Lingua::EN::Inflect qw ( PL PL_N PL_V PL_ADJ NO NUM
                             PL_eq PL_N_eq PL_V_eq PL_ADJ_eq
                             A AN
                             PART_PRES
                             ORD NUMWORDS
                             inflect classical
                             def_noun def_verb def_adj def_a def_an ); 

while( <> )
{
    # Process only nouns, and only if they are not proper nouns (that is,
    # they begin with a lower-case letter.)
    #
    if( /^([a-z].*)\t.*[N]/ ) {
        $word = $1;

        classical 0;
        process( $word, PL($word) );

        classical;
        process( $word, PL($word) );
    }
}

sub process {
    my $word = shift;
    my $plural = shift;

    # Skip silly cases like "a -> as"
    if( length($word) < 3 ) { return; }

    # Convert both to lower-case
    $word = lc($word);
    $plural = lc($plural);

    # If the plural already has an entry in the map, don't make another one
    if( $map{$plural} ne "" ) { return; }

    # There are a few cases where a plural word is in the source file but
    # isn't marked as such. Try to detect these.
    #
    if( (($plural . "s") eq $word) or (($plural . "es") eq $word) ) { 
        return;
    }

    # Okay, make a new entry.
    $map{$plural} = $word;
}

$x = 0;
foreach( sort keys %map ) {
    $plural = $_;
    $word = $map{$plural};
    print "$plural|$word\n";
}
