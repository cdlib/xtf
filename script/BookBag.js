//////////////////////////////////////////////////////////////////////////////
//
// BookBag: handles sending off asynchronous requests that store document
//          identifiers in session data.
//
//////////////////////////////////////////////////////////////////////////////

// Asynchronously add a book to the bag.
addToBag = function( ark, url ) 
{
  var target = document.getElementById( ark + "-add" );
  target.innerHTML = "Adding...";
  var loader = new net.AsyncLoader( url, 
      function() { target.innerHTML = this.req.responseText; },
      function() { target.innerHTML = "Error."; } );
}

// Asynchronously remove a book from the bag
removeFromBag = function( ark, url )
{
  var littleElement = document.getElementById( ark + "-remove" );
  var bigElement = document.getElementById( ark + "-main" );
  var countElement = document.getElementById( "itemCount" );
  littleElement.innerHTML = "Removing...";
  var loader = new net.AsyncLoader( url, 
      function() { bigElement.innerHTML = ""; --countElement.innerHTML; },
      function() { littleElement.innerHTML = "Error."; } );
}