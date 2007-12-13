//////////////////////////////////////////////////////////////////////////////
//
// BookBag: handles sending off asynchronous requests that store document
//          identifiers in session data.
//
//////////////////////////////////////////////////////////////////////////////

// Asynchronously add a book to the bag.
addToBag = function( ark, url ) 
{
  var target = document.getElementById( "add-" + ark );
  target.innerHTML = "Adding...";
  var loader = new net.AsyncLoader( url, 
      function() { target.innerHTML = this.req.responseText; },
      function() { target.innerHTML = "Error."; } );
}

// Asynchronously remove a book from the bag
removeFromBag = function( ark, url )
{
  var littleElement = document.getElementById( "remove-" + ark );
  var bigElement = document.getElementById( "main-" + ark );
  var countElement = document.getElementById( "itemCount" );
  littleElement.innerHTML = "Removing...";
  var loader = new net.AsyncLoader( url, 
      function() { bigElement.parentNode.removeChild(bigElement); --countElement.innerHTML; },
      function() { littleElement.innerHTML = "Error."; } );
}
