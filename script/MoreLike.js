//////////////////////////////////////////////////////////////////////////////
//
// MoreLike: handles sending off asynchronous requests that fetch "more
//           documents like this".
//
//////////////////////////////////////////////////////////////////////////////

// Asynchronously fetch more docs
moreLike = function( ark, url )
{
  var element = document.getElementById( ark + "-moreLike" );
  element.innerHTML = "Fetching...";
  var loader = new net.AsyncLoader( url, 
      function() { element.innerHTML = this.req.responseText; },
      function() { element.innerHTML = "Error."; } );
}